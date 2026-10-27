package com.examlens.service;

import com.examlens.dao.ExamDao;
import com.examlens.exception.AuthorizationException;
import com.examlens.exception.ResourceNotFoundException;
import com.examlens.exception.ValidationException;
import com.examlens.model.Exam;
import com.examlens.service.impl.ExamServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Unit tests for ExamService business rules, field validations,
 * and multi-tenant authorization logic using an in-memory ExamDao stub.
 */
public class ExamServiceTest {

    public static void main(String[] args) throws Exception {
        System.out.println("Running ExamServiceTest...");

        testCreateExamSuccess();
        testCreateExamNullOrBlankSubject();
        testCreateExamSubjectLengthBounds();
        testCreateExamPastDateTime();
        testCreateExamInvalidDailyStudyMinutes();
        testCreateExamNegativeTargetMarks();
        testCreateExamNullUserId();
        testGetExamSuccess();
        testGetExamUnauthorizedAccess();
        testGetExamNotFound();
        testGetExamsUserIsolation();

        System.out.println("[PASS] All ExamService tests passed successfully!");
    }

    private static void testCreateExamSuccess() throws Exception {
        InMemoryExamDao examDao = new InMemoryExamDao();
        ExamService examService = new ExamServiceImpl(examDao);

        Exam exam = new Exam();
        exam.setSubjectName("  Data Structures & Algorithms  ");
        exam.setExamDateTime(LocalDateTime.now().plusDays(14));
        exam.setDailyStudyMinutes(120);
        exam.setTargetMarks(new BigDecimal("95.50"));

        Exam created = examService.createExam(1L, exam);

        assertTrue(created.getId() != null, "Created exam ID should not be null.");
        assertTrue(Long.valueOf(1L).equals(created.getUserId()), "Exam user ID must be set to creator.");
        assertTrue("Data Structures & Algorithms".equals(created.getSubjectName()), "Subject name should be trimmed.");
        assertTrue("ACTIVE".equals(created.getStatus()), "Default status should be ACTIVE.");
    }

    private static void testCreateExamNullOrBlankSubject() {
        InMemoryExamDao examDao = new InMemoryExamDao();
        ExamService examService = new ExamServiceImpl(examDao);

        Exam exam = new Exam();
        exam.setSubjectName("   ");
        exam.setExamDateTime(LocalDateTime.now().plusDays(5));
        exam.setDailyStudyMinutes(60);

        try {
            examService.createExam(1L, exam);
            throw new AssertionError("Expected ValidationException for blank subject name.");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("Subject name is required"), "Expected subject required message.");
        }
    }

    private static void testCreateExamSubjectLengthBounds() {
        InMemoryExamDao examDao = new InMemoryExamDao();
        ExamService examService = new ExamServiceImpl(examDao);

        // Test length < 2
        Exam shortExam = new Exam();
        shortExam.setSubjectName("A");
        shortExam.setExamDateTime(LocalDateTime.now().plusDays(5));
        shortExam.setDailyStudyMinutes(60);

        try {
            examService.createExam(1L, shortExam);
            throw new AssertionError("Expected ValidationException for 1-char subject name.");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("between 2 and 150"), "Expected length bounds message.");
        }

        // Test length > 150
        Exam longExam = new Exam();
        longExam.setSubjectName("A".repeat(151));
        longExam.setExamDateTime(LocalDateTime.now().plusDays(5));
        longExam.setDailyStudyMinutes(60);

        try {
            examService.createExam(1L, longExam);
            throw new AssertionError("Expected ValidationException for >150 char subject name.");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("between 2 and 150"), "Expected length bounds message.");
        }
    }

    private static void testCreateExamPastDateTime() {
        InMemoryExamDao examDao = new InMemoryExamDao();
        ExamService examService = new ExamServiceImpl(examDao);

        Exam exam = new Exam();
        exam.setSubjectName("Computer Networks");
        exam.setExamDateTime(LocalDateTime.now().minusDays(1)); // Past
        exam.setDailyStudyMinutes(60);

        try {
            examService.createExam(1L, exam);
            throw new AssertionError("Expected ValidationException for past exam date/time.");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("must be set in the future"), "Expected future date message.");
        }
    }

    private static void testCreateExamInvalidDailyStudyMinutes() {
        InMemoryExamDao examDao = new InMemoryExamDao();
        ExamService examService = new ExamServiceImpl(examDao);

        // Test <= 0
        Exam zeroMinutes = new Exam();
        zeroMinutes.setSubjectName("Operating Systems");
        zeroMinutes.setExamDateTime(LocalDateTime.now().plusDays(10));
        zeroMinutes.setDailyStudyMinutes(0);

        try {
            examService.createExam(1L, zeroMinutes);
            throw new AssertionError("Expected ValidationException for 0 daily study minutes.");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("between 1 and 1440"), "Expected minutes bounds message.");
        }

        // Test > 1440 (more than 24 hours in a day)
        Exam excessMinutes = new Exam();
        excessMinutes.setSubjectName("Operating Systems");
        excessMinutes.setExamDateTime(LocalDateTime.now().plusDays(10));
        excessMinutes.setDailyStudyMinutes(1441);

        try {
            examService.createExam(1L, excessMinutes);
            throw new AssertionError("Expected ValidationException for 1441 daily study minutes.");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("between 1 and 1440"), "Expected minutes bounds message.");
        }
    }

    private static void testCreateExamNegativeTargetMarks() {
        InMemoryExamDao examDao = new InMemoryExamDao();
        ExamService examService = new ExamServiceImpl(examDao);

        Exam exam = new Exam();
        exam.setSubjectName("Software Engineering");
        exam.setExamDateTime(LocalDateTime.now().plusDays(20));
        exam.setDailyStudyMinutes(90);
        exam.setTargetMarks(new BigDecimal("-5.00"));

        try {
            examService.createExam(1L, exam);
            throw new AssertionError("Expected ValidationException for negative target marks.");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("cannot be negative"), "Expected non-negative marks message.");
        }
    }

    private static void testCreateExamNullUserId() {
        InMemoryExamDao examDao = new InMemoryExamDao();
        ExamService examService = new ExamServiceImpl(examDao);

        Exam exam = new Exam();
        exam.setSubjectName("Physics");
        exam.setExamDateTime(LocalDateTime.now().plusDays(10));
        exam.setDailyStudyMinutes(60);

        try {
            examService.createExam(null, exam);
            throw new AssertionError("Expected ValidationException for null user ID.");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("User ID is required"), "Expected user ID required message.");
        }
    }

    private static void testGetExamSuccess() throws Exception {
        InMemoryExamDao examDao = new InMemoryExamDao();
        ExamService examService = new ExamServiceImpl(examDao);

        Exam exam = new Exam();
        exam.setSubjectName("Compiler Design");
        exam.setExamDateTime(LocalDateTime.now().plusDays(30));
        exam.setDailyStudyMinutes(90);

        Exam created = examService.createExam(10L, exam);
        Exam retrieved = examService.getExam(created.getId(), 10L);

        assertTrue(retrieved != null, "Exam should be retrieved.");
        assertTrue(created.getId().equals(retrieved.getId()), "Retrieved exam ID should match.");
    }

    private static void testGetExamUnauthorizedAccess() throws Exception {
        InMemoryExamDao examDao = new InMemoryExamDao();
        ExamService examService = new ExamServiceImpl(examDao);

        // Student 10 creates an exam
        Exam exam = new Exam();
        exam.setSubjectName("Confidential Subject");
        exam.setExamDateTime(LocalDateTime.now().plusDays(15));
        exam.setDailyStudyMinutes(45);
        Exam created = examService.createExam(10L, exam);

        // Student 20 attempts to view Student 10's exam
        try {
            examService.getExam(created.getId(), 20L);
            throw new AssertionError("Expected AuthorizationException for cross-student access.");
        } catch (AuthorizationException expected) {
            assertTrue(expected.getMessage().contains("Access denied"), "Expected access denied message.");
        }
    }

    private static void testGetExamNotFound() throws Exception {
        InMemoryExamDao examDao = new InMemoryExamDao();
        ExamService examService = new ExamServiceImpl(examDao);

        try {
            examService.getExam(999L, 10L);
            throw new AssertionError("Expected ResourceNotFoundException for non-existent exam.");
        } catch (ResourceNotFoundException expected) {
            assertTrue(expected.getMessage().contains("not found"), "Expected not found message.");
        }
    }

    private static void testGetExamsUserIsolation() throws Exception {
        InMemoryExamDao examDao = new InMemoryExamDao();
        ExamService examService = new ExamServiceImpl(examDao);

        // Student 1 creates 2 exams
        Exam e1 = new Exam();
        e1.setSubjectName("Math 101");
        e1.setExamDateTime(LocalDateTime.now().plusDays(10));
        e1.setDailyStudyMinutes(60);
        examService.createExam(1L, e1);

        Exam e2 = new Exam();
        e2.setSubjectName("Math 102");
        e2.setExamDateTime(LocalDateTime.now().plusDays(20));
        e2.setDailyStudyMinutes(60);
        examService.createExam(1L, e2);

        // Student 2 creates 1 exam
        Exam e3 = new Exam();
        e3.setSubjectName("Biology 101");
        e3.setExamDateTime(LocalDateTime.now().plusDays(30));
        e3.setDailyStudyMinutes(60);
        examService.createExam(2L, e3);

        List<Exam> student1Exams = examService.getExams(1L);
        List<Exam> student2Exams = examService.getExams(2L);

        assertTrue(student1Exams.size() == 2, "Student 1 should see exactly 2 exams.");
        assertTrue(student2Exams.size() == 1, "Student 2 should see exactly 1 exam.");
        assertTrue("Biology 101".equals(student2Exams.get(0).getSubjectName()), "Student 2 should see Biology.");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }

    /**
     * In-memory test stub for ExamDao.
     */
    private static class InMemoryExamDao implements ExamDao {
        private final Map<Long, Exam> store = new HashMap<>();
        private long idSequence = 1L;

        @Override
        public Exam save(Exam exam) {
            exam.setId(idSequence++);
            exam.setCreatedAt(LocalDateTime.now());
            store.put(exam.getId(), exam);
            return exam;
        }

        @Override
        public Optional<Exam> findById(Long examId) {
            return Optional.ofNullable(store.get(examId));
        }

        @Override
        public Optional<Exam> findByIdAndUserId(Long examId, Long userId) {
            Exam exam = store.get(examId);
            if (exam != null && exam.getUserId().equals(userId)) {
                return Optional.of(exam);
            }
            return Optional.empty();
        }

        @Override
        public List<Exam> findAllByUserId(Long userId) {
            List<Exam> list = new ArrayList<>();
            for (Exam e : store.values()) {
                if (e.getUserId().equals(userId)) {
                    list.add(e);
                }
            }
            return list;
        }

        @Override
        public boolean update(Exam exam) {
            if (store.containsKey(exam.getId())) {
                store.put(exam.getId(), exam);
                return true;
            }
            return false;
        }

        @Override
        public boolean deleteByIdAndUserId(Long examId, Long userId) {
            Optional<Exam> opt = findByIdAndUserId(examId, userId);
            if (opt.isPresent()) {
                store.remove(examId);
                return true;
            }
            return false;
        }
    }
}
