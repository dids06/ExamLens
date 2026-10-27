package com.examlens.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

/** BCrypt password hashing. Cost 12 by default; configurable only for fast tests. */
public final class PasswordHasher {

    private static final int COST = Integer.getInteger("examlens.bcrypt.cost", 12);

    private PasswordHasher() {
    }

    public static String hash(String plain) {
        if (plain == null || plain.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        return BCrypt.withDefaults().hashToString(COST, plain.toCharArray());
    }

    public static boolean verify(String plain, String hash) {
        if (plain == null || hash == null || hash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.verifyer().verify(plain.toCharArray(), hash).verified;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Rejects weak passwords with an explicit reason. */
    public static String validateStrength(String password, String confirm) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters long.";
        }
        if (password.length() > 100) {
            return "Password must be at most 100 characters long.";
        }
        if (!password.equals(confirm)) {
            return "Passwords do not match.";
        }
        boolean letter = password.chars().anyMatch(Character::isLetter);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        if (!letter || !digit) {
            return "Password must contain at least one letter and one number.";
        }
        return null;
    }
}
