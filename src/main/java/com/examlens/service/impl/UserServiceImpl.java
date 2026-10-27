package com.examlens.service.impl;

import com.examlens.dao.UserDao;
import com.examlens.dao.impl.UserDaoImpl;
import com.examlens.exception.AuthenticationException;
import com.examlens.exception.DataAccessException;
import com.examlens.exception.ValidationException;
import com.examlens.model.User;
import com.examlens.service.UserService;
import com.examlens.util.PasswordUtil;
import com.examlens.util.ValidationUtil;

import java.util.Optional;

/**
 * Implementation of UserService implementing registration validation, password hashing, and login authentication.
 */
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    public UserServiceImpl() {
        this(new UserDaoImpl());
    }

    public UserServiceImpl(UserDao userDao) {
        if (userDao == null) {
            throw new IllegalArgumentException("UserDao cannot be null.");
        }
        this.userDao = userDao;
    }

    @Override
    public User register(String name, String email, String password, String confirmPassword)
            throws ValidationException, DataAccessException {

        ValidationUtil.validateRegistration(name, email, password, confirmPassword);

        String normalizedEmail = email.trim().toLowerCase();

        if (userDao.existsByEmail(normalizedEmail)) {
            throw new ValidationException("An account with this email address already exists. Please log in.");
        }

        String passwordHash = PasswordUtil.hashPassword(password);
        User user = new User(name.trim(), normalizedEmail, passwordHash);

        return userDao.save(user);
    }

    @Override
    public User authenticate(String email, String password)
            throws AuthenticationException, ValidationException, DataAccessException {

        ValidationUtil.validateLogin(email, password);

        String normalizedEmail = email.trim().toLowerCase();

        Optional<User> userOpt = userDao.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            // Constant-time behavior or generic message to prevent user enumeration
            throw new AuthenticationException("Invalid email or password.");
        }

        User user = userOpt.get();
        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password.");
        }

        return user;
    }

    @Override
    public Optional<User> findById(Long id) throws DataAccessException {
        if (id == null) {
            return Optional.empty();
        }
        return userDao.findById(id);
    }
}
