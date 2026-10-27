package com.examlens.service;

import com.examlens.dao.ExamDao;
import com.examlens.dao.PlanDao;
import com.examlens.dao.UserDao;
import com.examlens.model.Exam;
import com.examlens.model.ExamAvailability;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Exam lifecycle, declared availability, and the derived dated study capacity. */
public class ExamService {

    private final ExamDao exams;
    private final PlanDao plans;
    private final UserDao users;

    public ExamService(ExamDao exams, PlanDao plans, UserDao users) {
        this.exams = exams;
        this.plans = plans;
        this.users = users;
    }

    public long create(long userId, String subjectName, LocalDateTime examDatetime, String timezone,
                       Integer targetMarks, Integer totalMarks, String notes) {
        if (subjectName == null || subjectName.isBlank()) {
            throw new ServiceException("Enter the subject or exam name.");
        }
        if (examDatetime == null) {
            throw new ServiceException("Choose the exam date and time.");
        }
        if (examDatetime.isBefore(LocalDateTime.now().minusHours(1))) {
            throw new ServiceException("The exam date must be in the future.");
        }
        try {
            return exams.insert(userId, subjectName.trim(), examDatetime,
                    AuthService.validTimezone(timezone) ? timezone : "UTC", targetMarks, totalMarks,
                    blankToNull(notes));
        } catch (SQLException e) {
            throw new ServiceException("Could not create the exam.", e);
        }
    }

    public void update(long userId, long examId, String subjectName, LocalDateTime examDatetime, String timezone,
                       Integer targetMarks, Integer totalMarks, String notes, String status) {
        require(userId, examId);
        if (subjectName == null || subjectName.isBlank() || examDatetime == null) {
            throw new ServiceException("A subject name and exam date are required.");
        }
        String safeStatus = switch (status == null ? "" : status) {
            case "ARCHIVED", "COMPLETED" -> status;
            default -> "ACTIVE";
        };
        try {
            exams.update(examId, userId, subjectName.trim(), examDatetime,
                    AuthService.validTimezone(timezone) ? timezone : "UTC", targetMarks, totalMarks,
                    blankToNull(notes), safeStatus);
        } catch (SQLException e) {
            throw new ServiceException("Could not update the exam.", e);
        }
    }

    public Optional<Exam> find(long userId, long examId) {
        if (examId <= 0) {
            return Optional.empty();
        }
        try {
            return exams.find(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load the exam.", e);
        }
    }

    /** Resolves an exam id (which may be null/blank/malformed) to an owned exam, or fails safely. */
    public Exam require(long userId, Long examId) {
        if (examId == null || examId <= 0) {
            throw new ServiceException("Select a valid exam first.");
        }
        try {
            return exams.find(examId, userId)
                    .orElseThrow(() -> new ServiceException("That exam was not found in your account."));
        } catch (SQLException e) {
            throw new ServiceException("Could not load the exam.", e);
        }
    }

    public List<Exam> list(long userId) {
        try {
            return exams.listForUser(userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load your exams.", e);
        }
    }

    /** The exam a student is currently working on: the soonest upcoming, else the most recently created. */
    public Optional<Exam> active(long userId) {
        try {
            Optional<Exam> next = exams.nextExam(userId);
            if (next.isPresent()) {
                return next;
            }
            List<Exam> all = exams.listForUser(userId);
            return all.isEmpty() ? Optional.empty() : Optional.of(all.get(all.size() - 1));
        } catch (SQLException e) {
            throw new ServiceException("Could not load your exams.", e);
        }
    }

    public Exam requireActiveOptional(long userId, Long examIdParam) {
        if (examIdParam != null && examIdParam > 0) {
            return require(userId, examIdParam);
        }
        return active(userId).orElseThrow(() -> new ServiceException("Create an exam first."));
    }

    public void delete(long userId, long examId) {
        require(userId, examId);
        try {
            exams.delete(examId, userId);
        } catch (SQLException e) {
            throw new ServiceException("Could not delete the exam.", e);
        }
    }

    /**
     * Replaces the declared weekly/per-date availability. {@code dayOfWeek} follows ISO numbering
     * (1 = Monday .. 7 = Sunday). Values are validated to a sane range.
     */
    public void setWeeklyAvailability(long userId, long examId, Map<Integer, Integer> minutesByDayOfWeek) {
        require(userId, examId);
        try {
            exams.clearAvailability(examId);
            for (Map.Entry<Integer, Integer> entry : minutesByDayOfWeek.entrySet()) {
                int day = entry.getKey();
                int minutes = entry.getValue() == null ? 0 : entry.getValue();
                if (day < 1 || day > 7) {
                    continue;
                }
                if (minutes < 0 || minutes > 1440) {
                    throw new ServiceException("Daily study minutes must be between 0 and 1440.");
                }
                exams.addAvailability(examId, day, null, minutes, null);
            }
        } catch (SQLException e) {
            throw new ServiceException("Could not save your availability.", e);
        }
    }

    public List<ExamAvailability> availability(long examId) {
        try {
            return exams.availability(examId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load your availability.", e);
        }
    }

    /**
     * Expands declared availability into concrete dated capacity from today through the exam date.
     * Specific-date entries override the weekly pattern, which in turn overrides the account default.
     */
    public List<AvailabilityDay> availabilityDays(Exam exam) {
        LocalDate today = LocalDate.now();
        LocalDate examDate = exam.getExamDate();
        if (examDate == null || examDate.isBefore(today)) {
            return List.of();
        }
        Map<Integer, Integer> byDayOfWeek = new LinkedHashMap<>();
        Map<LocalDate, Integer> byDate = new LinkedHashMap<>();
        try {
            for (ExamAvailability a : exams.availability(exam.getId())) {
                if (a.getDayOfWeek() != null) {
                    byDayOfWeek.put(a.getDayOfWeek(), a.getAvailableMinutes());
                }
                if (a.getSpecificDate() != null) {
                    byDate.put(a.getSpecificDate(), a.getAvailableMinutes());
                }
            }
            int fallback = users.defaultStudyMinutes(exam.getUserId());
            List<AvailabilityDay> days = new ArrayList<>();
            for (LocalDate d = today; !d.isAfter(examDate); d = d.plusDays(1)) {
                Integer minutes = byDate.get(d);
                if (minutes == null) {
                    minutes = byDayOfWeek.get(d.getDayOfWeek().getValue());
                }
                if (minutes == null) {
                    minutes = fallback;
                }
                if (minutes != null && minutes > 0) {
                    days.add(new AvailabilityDay(d, minutes, null));
                }
            }
            return days;
        } catch (SQLException e) {
            throw new ServiceException("Could not compute your study availability.", e);
        }
    }

    public int totalAvailableMinutes(List<AvailabilityDay> days) {
        return days.stream().mapToInt(AvailabilityDay::minutes).sum();
    }

    public static LocalDateTime parseDateTime(String date, String time) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            LocalDate d = LocalDate.parse(date.trim());
            LocalTime t = (time == null || time.isBlank()) ? LocalTime.of(9, 0) : LocalTime.parse(time.trim());
            return LocalDateTime.of(d, t);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static Integer parsePositiveInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int v = Integer.parseInt(value.trim());
            return v < 0 ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
