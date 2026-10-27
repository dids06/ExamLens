package com.examlens.service;

import com.examlens.dao.UserDao;
import com.examlens.model.User;
import com.examlens.security.PasswordHasher;

import java.sql.SQLException;
import java.time.ZoneId;
import java.util.Optional;
import java.util.regex.Pattern;

/** Registration, sign-in and account settings. Passwords are only ever handled as BCrypt hashes. */
public class AuthService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]{1,64}@[^@\\s]{1,180}\\.[^@\\s]{2,}$");

    private final UserDao users;

    public AuthService(UserDao users) {
        this.users = users;
    }

    public long register(String email, String displayName, String password, String confirm, String timezone) {
        String normalized = normalizeEmail(email);
        if (normalized == null || !EMAIL.matcher(normalized).matches()) {
            throw new ServiceException("Enter a valid email address.");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new ServiceException("Enter your name.");
        }
        if (displayName.trim().length() > 120) {
            throw new ServiceException("Your name must be 120 characters or fewer.");
        }
        String strengthError = PasswordHasher.validateStrength(password, confirm);
        if (strengthError != null) {
            throw new ServiceException(strengthError);
        }
        try {
            if (users.findByEmail(normalized).isPresent()) {
                throw new ServiceException("An account with that email already exists. Sign in instead.");
            }
            String tz = validTimezone(timezone) ? timezone : "UTC";
            long id = users.insert(normalized, displayName.trim(), PasswordHasher.hash(password), tz);
            users.ensureSettings(id);
            return id;
        } catch (SQLException e) {
            throw new ServiceException("Could not create your account right now. Please try again.", e);
        }
    }

    public User authenticate(String email, String password) {
        String normalized = normalizeEmail(email);
        if (normalized == null || password == null || password.isEmpty()) {
            throw new ServiceException("Enter your email and password.");
        }
        try {
            Optional<User> found = users.findByEmail(normalized);
            // Always run a hash verification to reduce timing differences between unknown and wrong-password cases.
            String hash = found.map(User::getPasswordHash).orElse("$2a$12$0000000000000000000000000000000000000000000000000000");
            boolean ok = PasswordHasher.verify(password, hash);
            if (found.isEmpty() || !ok) {
                throw new ServiceException("Email or password is incorrect.");
            }
            users.touchLogin(found.get().getId());
            return found.get();
        } catch (SQLException e) {
            throw new ServiceException("Sign-in failed right now. Please try again.", e);
        }
    }

    public User require(long userId) {
        try {
            return users.find(userId).orElseThrow(() -> new ServiceException("Your account could not be found."));
        } catch (SQLException e) {
            throw new ServiceException("Could not load your account.", e);
        }
    }

    public void updateProfile(long userId, String displayName, String timezone, String theme) {
        if (displayName == null || displayName.isBlank()) {
            throw new ServiceException("Enter your name.");
        }
        if (displayName.trim().length() > 120) {
            throw new ServiceException("Your name must be 120 characters or fewer.");
        }
        String tz = validTimezone(timezone) ? timezone : "UTC";
        String safeTheme = switch (theme == null ? "" : theme) {
            case "light", "dark" -> theme;
            default -> "system";
        };
        try {
            users.updateProfile(userId, displayName.trim(), tz, safeTheme);
        } catch (SQLException e) {
            throw new ServiceException("Could not save your profile.", e);
        }
    }

    public void changePassword(long userId, String current, String next, String confirm) {
        User user = require(userId);
        if (!PasswordHasher.verify(current, user.getPasswordHash())) {
            throw new ServiceException("Your current password is incorrect.");
        }
        String strengthError = PasswordHasher.validateStrength(next, confirm);
        if (strengthError != null) {
            throw new ServiceException(strengthError);
        }
        try {
            users.updatePassword(userId, PasswordHasher.hash(next));
        } catch (SQLException e) {
            throw new ServiceException("Could not change your password.", e);
        }
    }

    /** Persists only the theme choice so the client-side toggle can sync the server without a full profile save. */
    public void updateTheme(long userId, String theme) {
        String safeTheme = switch (theme == null ? "" : theme) {
            case "light", "dark", "system" -> theme;
            default -> "system";
        };
        try {
            users.updateTheme(userId, safeTheme);
        } catch (SQLException e) {
            throw new ServiceException("Could not save your theme preference.", e);
        }
    }

    public void updateSettings(long userId, int defaultStudyMinutes, String depth, int retentionDays) {
        if (defaultStudyMinutes < 15 || defaultStudyMinutes > 720) {
            throw new ServiceException("Default daily study time must be between 15 and 720 minutes.");
        }
        String safeDepth = switch (depth == null ? "" : depth) {
            case "BRIEF", "STANDARD", "DEEP" -> depth;
            default -> "STANDARD";
        };
        if (retentionDays < 30 || retentionDays > 3650) {
            retentionDays = 365;
        }
        try {
            users.updateSettings(userId, defaultStudyMinutes, safeDepth, retentionDays);
        } catch (SQLException e) {
            throw new ServiceException("Could not save your settings.", e);
        }
    }

    public UserDao.Settings settings(long userId) {
        try {
            return users.settings(userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load your settings.", e);
        }
    }

    public void deleteAccount(long userId) {
        try {
            users.deleteAccount(userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not delete your account.", e);
        }
    }

    public static boolean validTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return false;
        }
        try {
            ZoneId.of(timezone.trim());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim().toLowerCase(java.util.Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }
}
