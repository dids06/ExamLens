package com.examlens.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void hashIsNotThePlainTextAndVerifies() {
        String hash = PasswordHasher.hash("Correct1Horse");
        assertNotEquals("Correct1Horse", hash);
        assertTrue(hash.startsWith("$2"));
        assertTrue(PasswordHasher.verify("Correct1Horse", hash));
    }

    @Test
    void differentPasswordsProduceDifferentHashesForTheSameInput() {
        String a = PasswordHasher.hash("Correct1Horse");
        String b = PasswordHasher.hash("Correct1Horse");
        assertNotEquals(a, b, "BCrypt must use a random salt per hash");
        assertTrue(PasswordHasher.verify("Correct1Horse", a));
        assertTrue(PasswordHasher.verify("Correct1Horse", b));
    }

    @Test
    void wrongPasswordDoesNotVerify() {
        String hash = PasswordHasher.hash("Correct1Horse");
        assertFalse(PasswordHasher.verify("wrong1Horse", hash));
    }

    @Test
    void malformedHashDoesNotThrowAndDoesNotVerify() {
        assertFalse(PasswordHasher.verify("anything", "not-a-bcrypt-hash"));
        assertFalse(PasswordHasher.verify("anything", ""));
        assertFalse(PasswordHasher.verify("anything", null));
    }

    @Test
    void emptyPasswordCannotBeHashed() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
    }

    @Test
    void strengthValidationRejectsWeakPasswords() {
        assertEquals("Password must be at least 8 characters long.",
                PasswordHasher.validateStrength("Ab1", "Ab1"));
        assertEquals("Passwords do not match.",
                PasswordHasher.validateStrength("abcdefg1", "abcdefg2"));
        assertEquals("Password must contain at least one letter and one number.",
                PasswordHasher.validateStrength("abcdefgh", "abcdefgh"));
        assertEquals("Password must contain at least one letter and one number.",
                PasswordHasher.validateStrength("12345678", "12345678"));
        assertNull(PasswordHasher.validateStrength("abcdefg1", "abcdefg1"));
    }
}
