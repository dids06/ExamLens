package com.examlens.web;

import com.examlens.model.Exam;
import com.examlens.model.ExamAvailability;
import com.examlens.service.ExamService;
import com.examlens.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exam lifecycle: create, edit, delete and declared weekly availability. */
@WebServlet(name = "exams", urlPatterns = {"/exams", "/exams/*"})
public class ExamServlet extends ServletSupport {

    private static final String[] DAY_NAMES = {
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"
    };

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = path(req);
        switch (path) {
            case "/exams" -> {
                req.setAttribute("exams", ctx().exams().list(userId(req)));
                req.setAttribute("activeExamId", ctx().exams().active(userId(req)).map(Exam::getId).orElse(null));
                render(req, res, "exams/list");
            }
            case "/exams/new" -> render(req, res, "exams/form");
            case "/exams/view" -> view(req, res);
            case "/exams/availability" -> availability(req, res);
            default -> {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                render(req, res, "error/404");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = path(req);
        switch (path) {
            case "/exams/create" -> create(req, res);
            case "/exams/update" -> update(req, res);
            case "/exams/delete" -> delete(req, res);
            case "/exams/availability" -> saveAvailability(req, res);
            default -> redirect(req, res, "/exams");
        }
    }

    private void create(HttpServletRequest req, HttpServletResponse res) throws IOException {
        action(req, res, "/exams/new", () -> {
            LocalDateTime when = ExamService.parseDateTime(Web.param(req, "examDate"), Web.param(req, "examTime"));
            long id = ctx().exams().create(userId(req), Web.param(req, "subjectName"), when,
                    Web.param(req, "timezone", "UTC"),
                    ExamService.parsePositiveInt(Web.param(req, "targetMarks")),
                    ExamService.parsePositiveInt(Web.param(req, "totalMarks")),
                    Web.param(req, "notes"));
            Web.success(req, "Exam created. Next, upload your syllabus and set your study time.");
            return "/exams/view?id=" + id;
        });
    }

    private void update(HttpServletRequest req, HttpServletResponse res) throws IOException {
        long examId = Web.longParam(req, "examId", 0);
        action(req, res, "/exams/view?id=" + examId, () -> {
            LocalDateTime when = ExamService.parseDateTime(Web.param(req, "examDate"), Web.param(req, "examTime"));
            ctx().exams().update(userId(req), examId, Web.param(req, "subjectName"), when,
                    Web.param(req, "timezone", "UTC"),
                    ExamService.parsePositiveInt(Web.param(req, "targetMarks")),
                    ExamService.parsePositiveInt(Web.param(req, "totalMarks")),
                    Web.param(req, "notes"), Web.param(req, "status", "ACTIVE"));
            Web.success(req, "Exam details updated.");
            return "/exams/view?id=" + examId;
        });
    }

    private void delete(HttpServletRequest req, HttpServletResponse res) throws IOException {
        long examId = Web.longParam(req, "examId", 0);
        action(req, res, "/exams/view?id=" + examId, () -> {
            ctx().exams().delete(userId(req), examId);
            Web.success(req, "Exam and all of its records were deleted.");
            return "/exams";
        });
    }

    private void saveAvailability(HttpServletRequest req, HttpServletResponse res) throws IOException {
        long examId = Web.longParam(req, "examId", 0);
        action(req, res, "/exams/availability?examId=" + examId, () -> {
            Map<Integer, Integer> minutes = new LinkedHashMap<>();
            for (int i = 0; i < DAY_NAMES.length; i++) {
                minutes.put(i + 1, Web.intParam(req, DAY_NAMES[i], 0));
            }
            ctx().exams().setWeeklyAvailability(userId(req), examId, minutes);
            Web.success(req, "Weekly availability saved. ExamLens will use it when planning.");
            return "/exams/availability?examId=" + examId;
        });
    }

    private void view(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Long examId = Web.nullableId(req, "id");
        if (examId == null || examId <= 0) {
            Web.error(req, "That exam was not found.");
            redirect(req, res, "/exams");
            return;
        }
        Exam exam = ctx().exams().require(userId(req), examId);
        req.setAttribute("exam", exam);
        req.setAttribute("topicCount", ctx().syllabus().countConfirmedTopics(userId(req), examId));
        req.setAttribute("documentCount", ctx().documents().list(userId(req), examId).size());
        req.setAttribute("hasPlan", ctx().plan().activeVersion(userId(req), examId).isPresent());
        req.setAttribute("availability", ctx().exams().availability(examId));
        render(req, res, "exams/view");
    }

    private void availability(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Exam exam = resolveExam(req, res);
        if (exam == null) {
            return;
        }
        Map<Integer, Integer> byDay = new LinkedHashMap<>();
        for (ExamAvailability a : ctx().exams().availability(exam.getId())) {
            if (a.getDayOfWeek() != null) {
                byDay.put(a.getDayOfWeek(), a.getAvailableMinutes());
            }
        }
        List<DayCell> days = new java.util.ArrayList<>();
        for (int i = 0; i < DAY_NAMES.length; i++) {
            String label = Character.toUpperCase(DAY_NAMES[i].charAt(0)) + DAY_NAMES[i].substring(1);
            days.add(new DayCell(i + 1, DAY_NAMES[i], label, byDay.getOrDefault(i + 1, 0)));
        }
        req.setAttribute("exam", exam);
        req.setAttribute("days", days);
        req.setAttribute("weeklyMinutes", days.stream().mapToInt(DayCell::getMinutes).sum());
        render(req, res, "exams/availability");
    }

    /** One editable weekly availability row (day number, form field name, display label, minutes). */
    public static final class DayCell {
        private final int day;
        private final String field;
        private final String label;
        private final int minutes;

        public DayCell(int day, String field, String label, int minutes) {
            this.day = day;
            this.field = field;
            this.label = label;
            this.minutes = minutes;
        }

        public int getDay() { return day; }
        public String getField() { return field; }
        public String getLabel() { return label; }
        public int getMinutes() { return minutes; }
    }

    private String path(HttpServletRequest req) {
        String uri = req.getRequestURI().substring(req.getContextPath().length());
        return uri.endsWith("/") && uri.length() > 1 ? uri.substring(0, uri.length() - 1) : uri;
    }
}
