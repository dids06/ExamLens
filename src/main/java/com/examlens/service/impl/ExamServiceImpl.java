package com.examlens.service.impl;

import com.examlens.dao.ExamDao;
import com.examlens.dao.impl.ExamDaoImpl;
import com.examlens.exception.AuthorizationException;
import com.examlens.exception.DataAccessException;
import com.examlens.exception.ResourceNotFoundException;
import com.examlens.exception.ValidationException;
import com.examlens.model.Exam;
import com.examlens.service.ExamService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of ExamService providing validation, ownership enforcement, and persistence orchestration.
 */
public class ExamServiceImpl implements ExamService {

    private static final int MIN_SUBJECT_LENGTH = 2;
    private static final int MAX_SUBJECT_LENGTH = 150;
    private static final int MIN_DAILY_MINUTES = 1;
    private static final int MAX_DAILY_MINUTES = 1440;

    private final ExamDao examDao;

    public ExamServiceImpl() {
        this(new ExamDaoImpl());
    }

    public ExamServiceImpl(ExamDao examDao) {
        if (examDao == null) {
            throw new IllegalArgumentException("ExamDao cannot be null.");
        }
        this.examDao = examDao;
    }

    @Override
    public Exam createExam(Long userId, Exam exam) throws ValidationException, DataAccessException {
        if (userId == null) {
            throw new ValidationException("User ID is required to create an exam workspace.");
        }
        if (exam == null) {
            throw new ValidationException("Exam details cannot be null.");
        }

        validateExamFields(exam);

        exam.setUserId(userId);
        if (exam.getStatus() == null || exam.getStatus().isBlank()) {
            exam.setStatus("ACTIVE");
        }

        return examDao.save(exam);
    }

    @Override
    public Exam getExam(Long examId, Long userId)
            throws ResourceNotFoundException, AuthorizationException, DataAccessException {

        if (examId == null) {
            throw new ResourceNotFoundException("Exam ID cannot be null.");
        }
        if (userId == null) {
            throw new AuthorizationException("Authentication required to access exam workspace.");
        }

        Optional<Exam> ownedExamOpt = examDao.findByIdAndUserId(examId, userId);
        if (ownedExamOpt.isPresent()) {
            return ownedExamOpt.get();
        }

        // Check if exam exists under another student's account
        Optional<Exam> anyExamOpt = examDao.findById(examId);
        if (anyExamOpt.isPresent()) {
            throw new AuthorizationException("Access denied: You do not have permission to access exam workspace #" + examId);
        }

        throw new ResourceNotFoundException("Exam workspace not found with ID #" + examId);
    }

    @Override
    public List<Exam> getExams(Long userId) throws DataAccessException {
        if (userId == null) {
            return List.of();
        }
        return examDao.findAllByUserId(userId);
    }

    @Override
    public void updateExam(Long examId, Long userId, Exam exam)
            throws ValidationException, ResourceNotFoundException, AuthorizationException, DataAccessException {

        getExam(examId, userId); // Verifies existence and ownership
        validateExamFields(exam);

        exam.setId(examId);
        exam.setUserId(userId);

        examDao.update(exam);
    }

    @Override
    public void deleteExam(Long examId, Long userId)
            throws ResourceNotFoundException, AuthorizationException, DataAccessException {

        getExam(examId, userId); // Verifies existence and ownership
        examDao.deleteByIdAndUserId(examId, userId);
    }

    private void validateExamFields(Exam exam) throws ValidationException {
        if (exam.getSubjectName() == null || exam.getSubjectName().isBlank()) {
            throw new ValidationException("Subject name is required.");
        }

        String subject = exam.getSubjectName().trim();
        if (subject.length() < MIN_SUBJECT_LENGTH || subject.length() > MAX_SUBJECT_LENGTH) {
            throw new ValidationException("Subject name must be between " + MIN_SUBJECT_LENGTH + " and " + MAX_SUBJECT_LENGTH + " characters.");
        }
        exam.setSubjectName(subject);

        if (exam.getExamDateTime() == null) {
            throw new ValidationException("Exam date and time are required.");
        }

        if (exam.getExamDateTime().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Exam date and time must be set in the future.");
        }

        if (exam.getDailyStudyMinutes() == null) {
            throw new ValidationException("Daily study minutes are required.");
        }

        if (exam.getDailyStudyMinutes() < MIN_DAILY_MINUTES || exam.getDailyStudyMinutes() > MAX_DAILY_MINUTES) {
            throw new ValidationException("Daily study minutes must be between " + MIN_DAILY_MINUTES + " and " + MAX_DAILY_MINUTES + " minutes.");
        }

        if (exam.getTargetMarks() != null && exam.getTargetMarks().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Target marks cannot be negative.");
        }
    }
}
