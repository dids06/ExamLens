package com.examlens.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Secure password hashing utility using PBKDF2 with HMAC-SHA256 and cryptographic salts.
 * Implemented using standard Java 17 APIs with zero external dependencies.
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
        // Prevent instantiation
    }

    /**
     * Hashes a plaintext password with a freshly generated random salt.
     * Output format: PBKDF2:iterations:saltBase64:hashBase64
     *
     * @param password plaintext password
     * @return formatted hash string
     * @throws IllegalArgumentException if password is null or blank
     */
    public static String hashPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank.");
        }

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);

        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);

        return "PBKDF2:" + ITERATIONS + ":" +
                Base64.getEncoder().encodeToString(salt) + ":" +
                Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifies a candidate plaintext password against a stored formatted hash string.
     * Uses constant-time comparison to prevent timing attacks.
     *
     * @param candidatePassword plaintext candidate password
     * @param storedHash        formatted hash string
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String candidatePassword, String storedHash) {
        if (candidatePassword == null || storedHash == null || candidatePassword.isBlank() || storedHash.isBlank()) {
            return false;
        }

        String[] parts = storedHash.split(":");
        if (parts.length != 4 || !"PBKDF2".equals(parts[0])) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

            byte[] actualHash = pbkdf2(candidatePassword.toCharArray(), salt, iterations, expectedHash.length * 8);

            // Constant-time comparison
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Cryptographic algorithm unavailable for password hashing: " + ALGORITHM, e);
        } finally {
            spec.clearPassword();
        }
    }
}
