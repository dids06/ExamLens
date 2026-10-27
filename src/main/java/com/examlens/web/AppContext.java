package com.examlens.web;

import com.examlens.ai.AiService;
import com.examlens.ai.AiServiceFactory;
import com.examlens.ai.AiServiceRunner;
import com.examlens.config.AppConfig;
import com.examlens.dao.AiJobDao;
import com.examlens.dao.DocumentDao;
import com.examlens.dao.ExamDao;
import com.examlens.dao.LearningDao;
import com.examlens.dao.PlanDao;
import com.examlens.dao.PracticeDao;
import com.examlens.dao.SyllabusDao;
import com.examlens.dao.UserDao;
import com.examlens.service.AssistantService;
import com.examlens.service.AuthService;
import com.examlens.service.DocumentExtractionService;
import com.examlens.service.DocumentService;
import com.examlens.service.ExamService;
import com.examlens.service.MaterialService;
import com.examlens.service.PracticeService;
import com.examlens.service.PyqService;
import com.examlens.service.ReadinessService;
import com.examlens.service.RevisionService;
import com.examlens.service.SessionService;
import com.examlens.service.StorageService;
import com.examlens.service.StudyPlanService;
import com.examlens.service.SyllabusService;
import com.examlens.service.TeacherGuidanceService;

/**
 * Application composition root. DAOs and services are stateless (all state lives in the database),
 * so a single shared instance is safe across requests. Created once at startup.
 */
public final class AppContext {

    private static volatile AppContext instance;

    private final AppConfig config;
    private final AiServiceRunner ai;

    private final AuthService auth;
    private final ExamService exams;
    private final DocumentService documents;
    private final SyllabusService syllabus;
    private final TeacherGuidanceService guidance;
    private final MaterialService materials;
    private final PyqService pyq;
    private final StudyPlanService plan;
    private final PracticeService practice;
    private final RevisionService revision;
    private final SessionService sessions;
    private final ReadinessService readiness;
    private final AssistantService assistant;

    private AppContext() {
        this.config = AppConfig.get();

        UserDao users = new UserDao();
        ExamDao examDao = new ExamDao();
        DocumentDao documentDao = new DocumentDao();
        SyllabusDao syllabusDao = new SyllabusDao();
        PlanDao planDao = new PlanDao();
        PracticeDao practiceDao = new PracticeDao();
        LearningDao learningDao = new LearningDao();
        AiJobDao aiJobDao = new AiJobDao();

        AiService aiService = AiServiceFactory.create(config);
        this.ai = new AiServiceRunner(aiService, config, aiJobDao);

        this.exams = new ExamService(examDao, planDao, users);
        StorageService storage = new StorageService(config);
        DocumentExtractionService extraction = new DocumentExtractionService(config);
        this.auth = new AuthService(users);
        this.documents = new DocumentService(documentDao, storage, extraction, exams);
        this.syllabus = new SyllabusService(documentDao, syllabusDao, exams, ai);
        this.guidance = new TeacherGuidanceService(documentDao, learningDao, exams, ai);
        this.materials = new MaterialService(documentDao, learningDao, syllabus, exams, ai);
        this.pyq = new PyqService(documentDao, learningDao, syllabus, exams, ai);
        this.plan = new StudyPlanService(planDao, learningDao, syllabus, exams, ai);
        this.practice = new PracticeService(practiceDao, syllabus, exams, ai);
        this.sessions = new SessionService(planDao, exams, plan, ai);
        this.revision = new RevisionService(learningDao, syllabus, exams, ai, aiJobDao);
        this.readiness = new ReadinessService(learningDao, syllabus, exams, practice, plan, sessions, ai, aiJobDao);
        this.assistant = new AssistantService(learningDao, exams, syllabus, ai, aiJobDao);
    }

    public static AppContext get() {
        AppContext local = instance;
        if (local == null) {
            synchronized (AppContext.class) {
                local = instance;
                if (local == null) {
                    local = new AppContext();
                    instance = local;
                }
            }
        }
        return local;
    }

    public AppConfig config() {
        return config;
    }

    public AiServiceRunner ai() {
        return ai;
    }

    public AuthService auth() {
        return auth;
    }

    public ExamService exams() {
        return exams;
    }

    public DocumentService documents() {
        return documents;
    }

    public SyllabusService syllabus() {
        return syllabus;
    }

    public TeacherGuidanceService guidance() {
        return guidance;
    }

    public MaterialService materials() {
        return materials;
    }

    public PyqService pyq() {
        return pyq;
    }

    public StudyPlanService plan() {
        return plan;
    }

    public PracticeService practice() {
        return practice;
    }

    public RevisionService revision() {
        return revision;
    }

    public SessionService sessions() {
        return sessions;
    }

    public ReadinessService readiness() {
        return readiness;
    }

    public AssistantService assistant() {
        return assistant;
    }
}
