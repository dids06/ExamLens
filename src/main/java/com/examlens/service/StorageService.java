package com.examlens.service;

import com.examlens.config.AppConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores uploads outside the public web root using generated safe filenames.
 * Enforces an extension allow-list and a configured size limit, and computes a
 * SHA-256 fingerprint while streaming so the whole file never has to be buffered.
 */
public class StorageService {

    private final AppConfig config;

    public StorageService(AppConfig config) {
        this.config = config;
    }

    public record StoredFile(String storedFilename, String originalFilename, String contentType,
                             long size, String sha256, Path path) {
    }

    public StoredFile save(InputStream in, String originalFilename, String declaredContentType) {
        String ext = extension(originalFilename);
        if (!DocumentExtractionService.ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ServiceException("Unsupported file type \"" + ext + "\". Allowed types: PDF, DOCX, TXT, PNG, JPG, JPEG.");
        }
        long maxBytes = (long) config.uploadMaxMb() * 1024 * 1024;
        Path dir = config.storageDir();
        String stored = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target;
        try {
            Files.createDirectories(dir);
            target = dir.resolve(stored).normalize();
            if (target.getParent() == null || !target.getParent().equals(dir.normalize())) {
                throw new ServiceException("Invalid upload location.");
            }
        } catch (IOException e) {
            throw new ServiceException("Could not prepare the upload directory. Check EXAMLENS_STORAGE_DIR.", e);
        }

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException("Cannot verify the uploaded file (SHA-256 unavailable).", e);
        }

        long total = 0;
        try (InputStream src = in;
             OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = src.read(buffer)) > 0) {
                total += read;
                if (total > maxBytes) {
                    out.close();
                    Files.deleteIfExists(target);
                    throw new ServiceException("The file exceeds the " + config.uploadMaxMb() + " MB upload limit.");
                }
                digest.update(buffer, 0, read);
                out.write(buffer, 0, read);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (IOException e) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // best-effort cleanup
            }
            throw new ServiceException("The upload could not be saved. Please try again.", e);
        }

        String contentType = declaredContentType == null || declaredContentType.isBlank()
                ? contentTypeOf(ext) : declaredContentType;
        return new StoredFile(stored, safeName(originalFilename), contentType, total, hex(digest.digest()), target);
    }

    /** Resolves a stored filename inside the storage directory, refusing path traversal. */
    public Path resolve(String storedFilename) {
        if (storedFilename == null || storedFilename.isBlank()) {
            throw new ServiceException("Missing stored file reference.");
        }
        Path dir = config.storageDir().normalize();
        Path target = dir.resolve(storedFilename).normalize();
        if (target.getParent() == null || !target.getParent().equals(dir)) {
            throw new ServiceException("Invalid stored file reference.");
        }
        return target;
    }

    public void delete(String storedFilename) {
        try {
            Files.deleteIfExists(resolve(storedFilename));
        } catch (IOException | ServiceException ignored) {
            // deletion is best-effort; the database record is the source of truth
        }
    }

    public static String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public static String safeName(String filename) {
        if (filename == null || filename.isBlank()) {
            return "upload";
        }
        String name = filename.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[\\p{Cntrl}]", "").trim();
        return name.isBlank() ? "upload" : (name.length() > 180 ? name.substring(name.length() - 180) : name);
    }

    private static String contentTypeOf(String ext) {
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt" -> "text/plain";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
