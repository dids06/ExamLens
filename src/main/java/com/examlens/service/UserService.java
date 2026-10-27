package com.examlens.service;

import com.examlens.exception.AuthenticationException;
import com.examlens.exception.DataAccessException;
import com.examlens.exception.ValidationException;
import com.examlens.model.User;

import java.util.Optional;

/**
 * Service interface for user registration, authentication, and lookup.
 */
public interface UserService {

    /**
     * Registers a new student account after validating format and ensuring email uniqueness.
     *
     * @param name            full name
     * @param email           email address
     * @param password        plaintext password
     * @param confirmPassword confirmation password
     * @return persisted User entity
     * @throws ValidationException if validation fails or email already exists
     * @throws DataAccessException if a database persistence error occurs
     */
    User register(String name, String email, String password, String confirmPassword)
            throws ValidationException, DataAccessException;

    /**
     * Authenticates a user with email and password.
     *
     * @param email    email address
     * @param password plaintext password
     * @return authenticated User entity
     * @throws AuthenticationException if email not found or password does not match
     * @throws ValidationException     if inputs are empty or malformed
     * @throws DataAccessException     if a database error occurs
     */
    User authenticate(String email, String password)
            throws AuthenticationException, ValidationException, DataAccessException;

    /**
     * Retrieves user by primary key ID.
     *
     * @param id user ID
     * @return Optional containing User if present
     * @throws DataAccessException if a database error occurs
     */
    Optional<User> findById(Long id) throws DataAccessException;
}
