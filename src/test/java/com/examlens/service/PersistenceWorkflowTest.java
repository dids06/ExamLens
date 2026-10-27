package com.examlens.service;

import com.examlens.ai.AiServiceRunner;
import com.examlens.ai.MockAiService;
import com.examlens.config.AppConfig;
import com.examlens.config.Db;
import com.examlens.config.MigrationRunner;
import com.examlens.dao.AiJobDao;
import com.examlens.dao.DocumentDao;
import com.examlens.dao.ExamDao;
import com.examlens.dao.LearningDao;
import com.examlens.dao.PlanDao;
import com.examlens.dao.SyllabusDao;
import com.examlens.dao.UserDao;
import com.examlens.model.StudyTask;
import com.examlens.model.Topic;
import com.examlens.model.User;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end persistence and ownership tests against an in-memory MySQL-compatible H2 database,
 * driven by the deterministic simulated AI provider. This exercises the real DAO SQL, the real
 * migration scripts, the AI job accounting, and the plan persistence path.
 */
class PersistenceWorkflowTest {

    private static final String URL =
            "jdbc:h2:mem:examlens_it;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private static AppConfig config;
    private static AuthService auth;
    private static ExamService exams;
    private static SyllabusService syllabus;
    private static StudyPlanService plan;
    private static SyllabusDao syllabusDao;
    private static UserDao userDao;

    @BeforeAll
    static void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(URL);
        ds.setUser("sa");
        ds.setPassword("");
        try (Connection c = DriverManager.getConnection(URL, "sa", "")) {
            MigrationRunner.apply(c);
        }
        Db.setDataSource(ds);

        config = AppConfig.of(Map.of("AI_RATE_LIMIT_PER_HOUR", "500", "AI_PROVIDER", "mock"));
        userDao = new UserDao();
        ExamDao examDao = new ExamDao();
        DocumentDao documentDao = new DocumentDao();
        syllabusDao = new SyllabusDao();
        PlanDao planDao = new PlanDao();
        LearningDao learningDao = new LearningDao();
        AiJobDao aiJobDao = new AiJobDao();
        AiServiceRunner ai = new AiServiceRunner(new MockAiService(), config, aiJobDao);

        auth = new AuthService(userDao);
        exams = new ExamService(examDao, planDao, userDao);
        syllabus = new SyllabusService(documentDao, syllabusDao, exams, ai);
        plan = new StudyPlanService(planDao, learningDao, syllabus, exams, ai);
    }

    @AfterAll
    static void tearDown() {
        Db.close();
    }

    private String email() {
        return "student" + ThreadLocalRandom.current().nextInt(1_000_000) + "@example.com";
    }

    @Test
    void registrationHashesThePasswordAndSignInWorks() {
        String email = email();
        long id = auth.register(email, "Ada Lovelace", "Correct1Horse", "Correct1Horse", "UTC");
        assertTrue(id > 0);

        User stored = auth.require(id);
        assertFalse(stored.getPasswordHash().contains("Correct1Horse"), "the raw password must never be stored");
        assertTrue(stored.getPasswordHash().startsWith("$2"));

        assertEquals(id, auth.authenticate(email, "Correct1Horse").getId());
        assertThrows(ServiceException.class, () -> auth.authenticate(email, "wrong1Horse"));
    }

    @Test
    void duplicateEmailAndWeakPasswordAreRejected() {
        String email = email();
        auth.register(email, "Grace Hopper", "Correct1Horse", "Correct1Horse", "UTC");
        assertThrows(ServiceException.class,
                () -> auth.register(email, "Someone Else", "Correct1Horse", "Correct1Horse", "UTC"));
        assertThrows(ServiceException.class,
                () -> auth.register(email(), "Weak Person", "short", "short", "UTC"));
    }

    @Test
    void themeChangeIsPersisted() {
        long id = auth.register(email(), "Themed User", "Correct1Horse", "Correct1Horse", "UTC");
        auth.updateTheme(id, "dark");
        assertEquals("dark", auth.require(id).getTheme());
        auth.updateTheme(id, "not-a-theme");
        assertEquals("system", auth.require(id).getTheme(), "invalid theme values fall back to system");
    }

    @Test
    void examsAreIsolatedBetweenAccounts() {
        long owner = auth.register(email(), "Owner", "Correct1Horse", "Correct1Horse", "UTC");
        long other = auth.register(email(), "Intruder", "Correct1Horse", "Correct1Horse", "UTC");
        long examId = exams.create(owner, "Computer Science", LocalDateTime.now().plusDays(10), "UTC", 80, 100, null);

        assertEquals(examId, exams.require(owner, examId).getId());
        ServiceException denied = assertThrows(ServiceException.class, () -> exams.require(other, examId));
        assertTrue(denied.getUserMessage().toLowerCase().contains("not found"));
        assertTrue(exams.list(other).isEmpty(), "another account must not see this exam in its list");
    }

    @Test
    void aiPlanWorkflowGeneratesPersistsAndUpdatesTasks() throws Exception {
        long userId = auth.register(email(), "Planner", "Correct1Horse", "Correct1Horse", "UTC");
        long examId = exams.create(userId, "Mathematics", LocalDateTime.now().plusDays(7).withHour(9).withMinute(0),
                "UTC", 70, 100, null);

        // Confirm a syllabus with two topics (the human confirmation step) before planning.
        int versionNumber = syllabusDao.nextVersionNumber(examId);
        long versionId = syllabusDao.insertVersion(examId, userId, versionNumber, "CONFIRMED", null, null,
                "Confirmed syllabus for the test", "[]");
        insertTopic(versionId, examId, "Algebra", "Unit 1", "HIGH", 60, 1);
        insertTopic(versionId, examId, "Geometry", "Unit 1", "MEDIUM", 60, 2);

        List<Topic> confirmed = syllabus.confirmedTopics(userId, examId);
        assertEquals(2, confirmed.size());

        long version = plan.generate(userId, examId);
        assertTrue(version > 0);

        StudyPlanService.PlanView view = plan.loadPlan(userId, examId).orElseThrow();
        assertFalse(view.getTasks().isEmpty(), "the persisted plan must contain scheduled tasks");
        assertEquals(0, view.getCompletedTasks());
        assertFalse(view.getDays().isEmpty(), "tasks must be grouped into dated days");
        assertTrue(view.getPlannedMinutes() >= 120, "both 60-minute topics must be scheduled");
        assertTrue(view.getVersion().getFeasibility() != null,
                "the AI-generated plan must record a feasibility verdict");

        StudyTask first = view.getTasks().get(0);
        plan.updateTaskStatus(userId, first.getId(), "COMPLETED");
        StudyPlanService.PlanView refreshed = plan.loadPlan(userId, examId).orElseThrow();
        assertEquals(1, refreshed.getCompletedTasks());
        assertTrue(refreshed.getCompletionPercent() > 0);
    }

    @Test
    void availabilityAndFeasibilityUseTheRecommendedProvider() {
        long userId = auth.register(email(), "Feasible", "Correct1Horse", "Correct1Horse", "UTC");
        long examId = exams.create(userId, "Physics", LocalDateTime.now().plusDays(5), "UTC", null, 100, null);
        int versionNumber;
        try {
            versionNumber = syllabusDao.nextVersionNumber(examId);
            long versionId = syllabusDao.insertVersion(examId, userId, versionNumber, "CONFIRMED", null, null, "s", "[]");
            insertTopic(versionId, examId, "Mechanics", "Unit 1", "HIGH", 60, 1);
        } catch (Exception e) {
            throw new AssertionError(e);
        }

        var outcome = plan.assessFeasibility(userId, examId);
        assertTrue(List.of("FEASIBLE", "TIGHT", "INFEASIBLE").contains(outcome.getData().feasibility));
        assertTrue(outcome.isSimulated(), "the deterministic provider must be reported as simulated");
    }

    private void insertTopic(long versionId, long examId, String title, String unit, String importance,
                             int minutes, int order) throws Exception {
        Topic topic = new Topic();
        topic.setSyllabusVersionId(versionId);
        topic.setExamId(examId);
        topic.setUnitName(unit);
        topic.setTitle(title);
        topic.setImportance(importance);
        topic.setEstimatedMinutes(minutes);
        topic.setDisplayOrder(order);
        topic.setNeedsReview(false);
        syllabusDao.insertTopic(topic);
    }
}
