package com.examlens.util;

/**
 * Focused unit test for PasswordUtil verification, salt uniqueness, and format compliance.
 */
public class PasswordUtilTest {

    public static void main(String[] args) {
        System.out.println("Running PasswordUtilTest...");

        testHashAndVerifySuccess();
        testWrongPasswordFails();
        testSaltUniqueness();
        testMalformedHashFailsGracefully();
        testEmptyPasswordThrows();

        System.out.println("[PASS] All PasswordUtil tests passed successfully!");
    }

    private static void testHashAndVerifySuccess() {
        String rawPassword = "SecurePassword123!";
        String hash = PasswordUtil.hashPassword(rawPassword);

        assertTrue(hash != null && hash.startsWith("PBKDF2:65536:"), "Hash format should begin with PBKDF2:65536:");
        assertTrue(PasswordUtil.verifyPassword(rawPassword, hash), "Correct password must verify to true.");
    }

    private static void testWrongPasswordFails() {
        String rawPassword = "CorrectPassword123";
        String hash = PasswordUtil.hashPassword(rawPassword);

        assertTrue(!PasswordUtil.verifyPassword("WrongPassword123", hash), "Incorrect password must verify to false.");
        assertTrue(!PasswordUtil.verifyPassword("", hash), "Blank password must verify to false.");
        assertTrue(!PasswordUtil.verifyPassword(null, hash), "Null password must verify to false.");
    }

    private static void testSaltUniqueness() {
        String password = "SamePasswordForBoth";
        String hash1 = PasswordUtil.hashPassword(password);
        String hash2 = PasswordUtil.hashPassword(password);

        assertTrue(!hash1.equals(hash2), "Two hashes of the same password must have distinct salts.");
        assertTrue(PasswordUtil.verifyPassword(password, hash1), "Hash 1 must verify.");
        assertTrue(PasswordUtil.verifyPassword(password, hash2), "Hash 2 must verify.");
    }

    private static void testMalformedHashFailsGracefully() {
        assertTrue(!PasswordUtil.verifyPassword("test", "not:a:valid:hash"), "Malformed hash must fail gracefully.");
        assertTrue(!PasswordUtil.verifyPassword("test", ""), "Empty hash must fail gracefully.");
        assertTrue(!PasswordUtil.verifyPassword("test", null), "Null hash must fail gracefully.");
    }

    private static void testEmptyPasswordThrows() {
        try {
            PasswordUtil.hashPassword("");
            throw new AssertionError("Expected IllegalArgumentException for blank password.");
        } catch (IllegalArgumentException expected) {
            // Success
        }

        try {
            PasswordUtil.hashPassword(null);
            throw new AssertionError("Expected IllegalArgumentException for null password.");
        } catch (IllegalArgumentException expected) {
            // Success
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Test assertion failed: " + message);
        }
    }
}
