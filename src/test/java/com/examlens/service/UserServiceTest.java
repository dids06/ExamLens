package com.examlens.service;

import com.examlens.dao.UserDao;
import com.examlens.exception.AuthenticationException;
import com.examlens.exception.ValidationException;
import com.examlens.model.User;
import com.examlens.service.impl.UserServiceImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Focused unit test verifying UserService business rules, validation constraints,
 * and authentication logic without requiring a live MySQL database.
 */
public class UserServiceTest {

    public static void main(String[] args) {
        System.out.println("Running UserServiceTest...");

        testRegistrationSuccess();
        testPasswordMismatch();
        testShortPassword();
        testInvalidEmail();
        testDuplicateEmail();
        testAuthenticationSuccessAndFailure();

        System.out.println("[PASS] All UserService tests passed successfully!");
    }

    private static void testRegistrationSuccess() {
        InMemoryUserDao userDao = new InMemoryUserDao();
        UserService userService = new UserServiceImpl(userDao);

        try {
            User user = userService.register("Alice Student", "alice@example.com", "Password123!", "Password123!");
            assertTrue(user.getId() != null, "User ID should be generated.");
            assertTrue("alice@example.com".equals(user.getEmail()), "Email should match.");
            assertTrue(user.getPasswordHash() != null && user.getPasswordHash().startsWith("PBKDF2:"), "Password should be hashed.");
        } catch (Exception e) {
            throw new AssertionError("Registration should succeed: " + e.getMessage());
        }
    }

    private static void testPasswordMismatch() {
        InMemoryUserDao userDao = new InMemoryUserDao();
        UserService userService = new UserServiceImpl(userDao);

        try {
            userService.register("Bob", "bob@example.com", "Password123!", "DifferentPass123!");
            throw new AssertionError("Expected ValidationException on password mismatch.");
        } catch (ValidationException expected) {
            assertTrue(expected.getMessage().contains("do not match"), "Expected message about mismatch.");
        }
    }

    private static void testShortPassword() {
        InMemoryUserDao userDao = new InMemoryUserDao();
        UserService userService = new UserServiceImpl(userDao);

        try {
            userService.register("Bob", "bob@example.com", "short", "short");
            throw new AssertionError("Expected ValidationException for short password.");
        } catch (ValidationException expected) {
            assertTrue(expected.getMessage().contains("at least 8 characters"), "Expected message about length.");
        }
    }

    private static void testInvalidEmail() {
        InMemoryUserDao userDao = new InMemoryUserDao();
        UserService userService = new UserServiceImpl(userDao);

        try {
            userService.register("Charlie", "not-an-email", "Password123!", "Password123!");
            throw new AssertionError("Expected ValidationException for invalid email.");
        } catch (ValidationException expected) {
            assertTrue(expected.getMessage().contains("valid email"), "Expected message about email format.");
        }
    }

    private static void testDuplicateEmail() {
        InMemoryUserDao userDao = new InMemoryUserDao();
        UserService userService = new UserServiceImpl(userDao);

        try {
            userService.register("Dave", "dave@example.com", "Password123!", "Password123!");
            userService.register("Dave 2", "dave@example.com", "Password123!", "Password123!");
            throw new AssertionError("Expected ValidationException for duplicate email.");
        } catch (ValidationException expected) {
            assertTrue(expected.getMessage().contains("already exists"), "Expected duplicate message.");
        }
    }

    private static void testAuthenticationSuccessAndFailure() {
        InMemoryUserDao userDao = new InMemoryUserDao();
        UserService userService = new UserServiceImpl(userDao);

        try {
            userService.register("Eve", "eve@example.com", "SecretPass123!", "SecretPass123!");

            // Test successful authentication
            User authenticated = userService.authenticate("eve@example.com", "SecretPass123!");
            assertTrue(authenticated != null && "eve@example.com".equals(authenticated.getEmail()), "Authentication should succeed.");

            // Test failure with incorrect password
            try {
                userService.authenticate("eve@example.com", "WrongPassword!");
                throw new AssertionError("Expected AuthenticationException on wrong password.");
            } catch (AuthenticationException expected) {
                // Success
            }

            // Test failure with non-existent user
            try {
                userService.authenticate("unknown@example.com", "SecretPass123!");
                throw new AssertionError("Expected AuthenticationException on unknown user.");
            } catch (AuthenticationException expected) {
                // Success
            }

        } catch (Exception e) {
            throw new AssertionError("Unexpected error during auth test: " + e.getMessage());
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Test assertion failed: " + message);
        }
    }

    /**
     * In-memory test stub for UserDao.
     */
    private static class InMemoryUserDao implements UserDao {
        private final Map<Long, User> byId = new HashMap<>();
        private final Map<String, User> byEmail = new HashMap<>();
        private long idSequence = 1L;

        @Override
        public User save(User user) {
            user.setId(idSequence++);
            byId.put(user.getId(), user);
            byEmail.put(user.getEmail().toLowerCase(), user);
            return user;
        }

        @Override
        public Optional<User> findById(Long id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.ofNullable(byEmail.get(email.toLowerCase()));
        }

        @Override
        public boolean existsByEmail(String email) {
            return byEmail.containsKey(email.toLowerCase());
        }
    }
}
