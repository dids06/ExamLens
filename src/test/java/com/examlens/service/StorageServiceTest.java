package com.examlens.service;

import com.examlens.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageServiceTest {

    private StorageService storageFor(Path dir) {
        return new StorageService(AppConfig.of(Map.of("EXAMLENS_STORAGE_DIR", dir.toString())));
    }

    @Test
    void unsupportedFileTypesAreRejected(@TempDir Path dir) {
        StorageService storage = storageFor(dir);
        ServiceException error = assertThrows(ServiceException.class, () -> storage.save(
                new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)), "malware.exe", "application/octet-stream"));
        assertTrue(error.getUserMessage().toLowerCase().contains("unsupported"));
    }

    @Test
    void storedFileGetsSafeNameAndMatchingSha256(@TempDir Path dir) throws Exception {
        StorageService storage = storageFor(dir);
        byte[] content = "Chapter 1: Algebra\nPolynomials and factorisation.".getBytes(StandardCharsets.UTF_8);

        var stored = storage.save(new ByteArrayInputStream(content),
                "..\\..\\evil path\\My Syllabus!.txt", "text/plain");

        assertEquals("My Syllabus!.txt", stored.originalFilename(), "directory components must be stripped");
        assertTrue(stored.storedFilename().matches("[0-9a-f]{32}\\.txt"), "stored name must be a generated safe name");
        assertEquals(content.length, stored.size());
        assertEquals(sha256Hex(content), stored.sha256());
        assertTrue(Files.exists(dir.resolve(stored.storedFilename())));
    }

    @Test
    void oversizeFilesAreRejectedAndRemoved(@TempDir Path dir) {
        StorageService storage = new StorageService(
                AppConfig.of(Map.of("EXAMLENS_STORAGE_DIR", dir.toString(), "UPLOAD_MAX_MB", "1")));
        byte[] big = new byte[1024 * 1024 + 64];

        assertThrows(ServiceException.class,
                () -> storage.save(new ByteArrayInputStream(big), "big.txt", "text/plain"));
        try {
            assertEquals(0, Files.list(dir).count(), "the rejected partial upload must be cleaned up");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void resolveRefusesPathTraversal(@TempDir Path dir) {
        StorageService storage = storageFor(dir);
        assertThrows(ServiceException.class, () -> storage.resolve("../../etc/passwd"));
        assertThrows(ServiceException.class, () -> storage.resolve("sub/dir/file.txt"));
        assertThrows(ServiceException.class, () -> storage.resolve(""));
    }

    @Test
    void extensionExtractionIsDefensive() {
        assertEquals("pdf", StorageService.extension("file.PDF"));
        assertEquals("", StorageService.extension("noextension"));
        assertEquals("", StorageService.extension("trailingdot."));
        assertEquals("txt", StorageService.extension("weird.name.with.dots.txt"));
        assertEquals("", StorageService.extension(null));
    }

    @Test
    void safeNameRemovesControlCharactersAndDefaultsBlankNames() {
        assertEquals("upload", StorageService.safeName("   "));
        assertEquals("upload", StorageService.safeName(null));
        assertFalse(StorageService.safeName("a\u0000b.txt").contains("\u0000"));
    }

    private static String sha256Hex(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
