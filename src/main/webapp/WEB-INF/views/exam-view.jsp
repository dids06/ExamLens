<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.examlens.model.User" %>
<%@ page import="com.examlens.model.Exam" %>
<%@ page import="com.examlens.model.Topic" %>
<%@ page import="com.examlens.model.StudyPlan" %>
<%@ page import="com.examlens.model.StudyTask" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%!
    private String escapeHtml(Object obj) {
        if (obj == null) return "";
        return obj.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
    private static final DateTimeFormatter TASK_START_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM dd • hh:mm a");
    private static final DateTimeFormatter TASK_TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
%>
<%
    User currentUser = (User) session.getAttribute("user");
    Exam exam = (Exam) request.getAttribute("exam");
    @SuppressWarnings("unchecked")
    List<Topic> topics = (List<Topic>) request.getAttribute("topics");
    StudyPlan currentPlan = (StudyPlan) request.getAttribute("currentPlan");

    int totalMinutes = 0;
    if (topics != null) {
        for (Topic t : topics) {
            if (t.getEstimatedMinutes() != null) {
                totalMinutes += t.getEstimatedMinutes();
            }
        }
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= exam != null ? escapeHtml(exam.getSubjectName()) : "Exam Workspace" %> - ExamLens</title>
    <style>
        :root {
            --primary: #2563eb;
            --primary-hover: #1d4ed8;
            --bg: #f8fafc;
            --card-bg: #ffffff;
            --text: #0f172a;
            --text-muted: #64748b;
            --border: #e2e8f0;
            --success: #16a34a;
            --success-bg: #dcfce7;
            --success-border: #bbf7d0;
            --success-text: #15803d;
            --warning-bg: #fffbeb;
            --warning-border: #fef3c7;
            --warning-text: #b45309;
            --error-bg: #fef2f2;
            --error-border: #fecaca;
            --error-text: #b91c1c;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background-color: var(--bg);
            color: var(--text);
            min-height: 100vh;
        }

        .navbar {
            background-color: var(--card-bg);
            border-bottom: 1px solid var(--border);
            padding: 0.875rem 2rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .brand {
            font-size: 1.35rem;
            font-weight: 700;
            color: var(--primary);
            text-decoration: none;
            letter-spacing: -0.025em;
        }

        .container {
            max-width: 1050px;
            margin: 2rem auto;
            padding: 0 1.5rem;
        }

        .breadcrumb {
            margin-bottom: 1rem;
            font-size: 0.875rem;
        }

        .breadcrumb a {
            color: var(--primary);
            text-decoration: none;
            font-weight: 500;
        }

        .breadcrumb a:hover {
            text-decoration: underline;
        }

        .alert {
            padding: 0.875rem 1.25rem;
            border-radius: 8px;
            font-size: 0.9rem;
            margin-bottom: 1.5rem;
        }

        .alert-error {
            background-color: var(--error-bg);
            border: 1px solid var(--error-border);
            color: var(--error-text);
        }

        .alert-success {
            background-color: var(--success-bg);
            border: 1px solid var(--success-border);
            color: var(--success-text);
        }

        .alert-warning {
            background-color: var(--warning-bg);
            border: 1px solid var(--warning-border);
            color: var(--warning-text);
        }

        .exam-header-card {
            background: var(--card-bg);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 1.75rem 2rem;
            margin-bottom: 1.75rem;
            box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
        }

        .exam-title-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 1rem;
            margin-bottom: 1rem;
        }

        .exam-title {
            font-size: 1.65rem;
            font-weight: 700;
            letter-spacing: -0.02em;
        }

        .status-badge {
            background-color: #e0f2fe;
            color: #0369a1;
            font-size: 0.8rem;
            font-weight: 600;
            padding: 0.25rem 0.65rem;
            border-radius: 9999px;
            text-transform: uppercase;
        }

        .exam-meta-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 1rem;
            padding-top: 1rem;
            border-top: 1px solid var(--border);
        }

        .meta-item {
            font-size: 0.875rem;
        }

        .meta-label {
            color: var(--text-muted);
            font-size: 0.75rem;
            text-transform: uppercase;
            font-weight: 600;
            margin-bottom: 0.2rem;
        }

        .meta-value {
            font-weight: 600;
            color: var(--text);
        }

        /* Plan Generator Banner Card */
        .plan-cta-card {
            background: #eff6ff;
            border: 1px solid #bfdbfe;
            border-radius: 12px;
            padding: 1.5rem 2rem;
            margin-bottom: 1.75rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 1.25rem;
        }

        .plan-cta-title {
            font-size: 1.2rem;
            font-weight: 700;
            color: #1e3a8a;
            margin-bottom: 0.35rem;
        }

        .plan-cta-desc {
            font-size: 0.875rem;
            color: #3b82f6;
            max-width: 580px;
            line-height: 1.4;
        }

        .btn-generate-plan {
            padding: 0.75rem 1.5rem;
            background-color: var(--primary);
            color: white;
            border: none;
            border-radius: 8px;
            font-size: 0.95rem;
            font-weight: 600;
            cursor: pointer;
            transition: background-color 0.15s ease;
            white-space: nowrap;
        }

        .btn-generate-plan:hover {
            background-color: var(--primary-hover);
        }

        .btn-generate-plan:disabled {
            background-color: #94a3b8;
            cursor: not-allowed;
        }

        /* Study Plan Display Card */
        .plan-section-card {
            background: var(--card-bg);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 1.75rem;
            margin-bottom: 1.75rem;
            box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
        }

        .plan-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 0.75rem;
            margin-bottom: 1.25rem;
            padding-bottom: 1rem;
            border-bottom: 1px solid var(--border);
        }

        .plan-title-box {
            display: flex;
            align-items: center;
            gap: 0.75rem;
        }

        .plan-title {
            font-size: 1.25rem;
            font-weight: 700;
        }

        .plan-version-badge {
            background-color: #dbeafe;
            color: #1e40af;
            font-size: 0.75rem;
            font-weight: 700;
            padding: 0.2rem 0.55rem;
            border-radius: 6px;
        }

        .plan-meta-stats {
            display: flex;
            gap: 1.25rem;
            font-size: 0.85rem;
            color: var(--text-muted);
        }

        .plan-meta-stats strong {
            color: var(--text);
        }

        .tasks-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 0.875rem;
            margin-top: 0.5rem;
        }

        .tasks-table th {
            text-align: left;
            padding: 0.75rem 1rem;
            background-color: #f8fafc;
            color: var(--text-muted);
            font-size: 0.75rem;
            text-transform: uppercase;
            font-weight: 600;
            border-bottom: 1px solid var(--border);
        }

        .tasks-table td {
            padding: 0.875rem 1rem;
            border-bottom: 1px solid var(--border);
            vertical-align: middle;
        }

        .tasks-table tr:last-child td {
            border-bottom: none;
        }

        .tasks-table tr:hover {
            background-color: #f8fafc;
        }

        .task-date-col {
            white-space: nowrap;
            font-weight: 600;
            color: #334155;
        }

        .task-time-sub {
            font-size: 0.75rem;
            color: var(--text-muted);
            font-weight: normal;
        }

        .task-topic-name {
            font-weight: 600;
            color: var(--text);
        }

        .task-sub-arrow {
            color: var(--primary);
            margin-right: 0.25rem;
            font-weight: bold;
        }

        .badge-status {
            font-size: 0.7rem;
            font-weight: 600;
            padding: 0.2rem 0.5rem;
            border-radius: 4px;
            text-transform: uppercase;
        }

        .status-PLANNED {
            background-color: #f1f5f9;
            color: #475569;
        }

        .status-IN_PROGRESS {
            background-color: #fef3c7;
            color: #b45309;
        }

        .status-COMPLETED {
            background-color: #dcfce7;
            color: #15803d;
        }

        /* Workspace Grid (Topics & Form) */
        .workspace-grid {
            display: grid;
            grid-template-columns: 1fr 340px;
            gap: 1.5rem;
        }

        @media (max-width: 860px) {
            .workspace-grid {
                grid-template-columns: 1fr;
            }
        }

        .panel-card {
            background: var(--card-bg);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 1.75rem;
            box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
        }

        .panel-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 1.25rem;
            padding-bottom: 0.75rem;
            border-bottom: 1px solid var(--border);
        }

        .panel-title {
            font-size: 1.15rem;
            font-weight: 700;
        }

        .topic-list {
            list-style: none;
            display: flex;
            flex-direction: column;
            gap: 0.65rem;
        }

        .topic-item {
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 0.875rem 1rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            background: #fff;
            transition: border-color 0.15s ease;
        }

        .topic-item:hover {
            border-color: #cbd5e1;
        }

        .topic-item.subtopic {
            margin-left: 1.5rem;
            background: #f8fafc;
            border-left: 3px solid var(--primary);
        }

        .topic-name {
            font-weight: 600;
            font-size: 0.95rem;
            margin-bottom: 0.2rem;
        }

        .topic-sub-indicator {
            color: var(--text-muted);
            font-weight: normal;
            font-size: 0.85rem;
            margin-right: 0.35rem;
        }

        .topic-badges {
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }

        .badge-priority {
            font-size: 0.7rem;
            font-weight: 600;
            padding: 0.2rem 0.5rem;
            border-radius: 4px;
            text-transform: uppercase;
        }

        .priority-LOW {
            background-color: #f1f5f9;
            color: #475569;
        }

        .priority-MEDIUM {
            background-color: #eff6ff;
            color: #1d4ed8;
        }

        .priority-HIGH {
            background-color: #fff7ed;
            color: #c2410c;
        }

        .priority-TEACHER_IMPORTANT {
            background-color: #fdf4ff;
            color: #86198f;
            border: 1px solid #f5d0fe;
        }

        .topic-est-time {
            font-size: 0.8rem;
            color: var(--text-muted);
            font-weight: 500;
        }

        .form-group {
            margin-bottom: 1.1rem;
        }

        label {
            display: block;
            font-size: 0.825rem;
            font-weight: 600;
            margin-bottom: 0.3rem;
            color: var(--text);
        }

        input[type="text"],
        input[type="number"],
        select {
            width: 100%;
            padding: 0.6rem 0.75rem;
            font-size: 0.9rem;
            border: 1px solid var(--border);
            border-radius: 6px;
            background-color: #fff;
            color: var(--text);
            outline: none;
            transition: border-color 0.15s ease;
        }

        input[type="text"]:focus,
        input[type="number"]:focus,
        select:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
        }

        .btn-add-topic {
            width: 100%;
            padding: 0.65rem;
            background-color: var(--primary);
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 0.9rem;
            font-weight: 600;
            cursor: pointer;
            transition: background-color 0.15s ease;
            margin-top: 0.5rem;
        }

        .btn-add-topic:hover {
            background-color: var(--primary-hover);
        }

        .empty-topics {
            text-align: center;
            padding: 2.5rem 1rem;
            color: var(--text-muted);
            font-size: 0.9rem;
        }
    </style>
</head>
<body>

<header class="navbar">
    <a href="<%= request.getContextPath() %>/dashboard" class="brand">ExamLens</a>
    <span style="font-size: 0.875rem; color: var(--text-muted);">
        <%= currentUser != null ? escapeHtml(currentUser.getEmail()) : "" %>
    </span>
</header>

<main class="container">
    <div class="breadcrumb">
        <a href="<%= request.getContextPath() %>/dashboard">&larr; Back to Dashboard</a>
    </div>

    <% if (request.getAttribute("errorMessage") != null) { %>
        <div class="alert alert-error">
            <%= escapeHtml(request.getAttribute("errorMessage")) %>
        </div>
    <% } %>

    <% if (request.getAttribute("successMessage") != null) { %>
        <div class="alert alert-success">
            <%= escapeHtml(request.getAttribute("successMessage")) %>
        </div>
    <% } %>

    <% if (exam != null) { %>
        <!-- 1. Exam Header Details -->
        <div class="exam-header-card">
            <div class="exam-title-row">
                <h1 class="exam-title"><%= escapeHtml(exam.getSubjectName()) %></h1>
                <span class="status-badge"><%= escapeHtml(exam.getStatus()) %></span>
            </div>
            <div class="exam-meta-grid">
                <div class="meta-item">
                    <div class="meta-label">Exam Date & Time</div>
                    <div class="meta-value">
                        <%= exam.getExamDateTime() != null ? exam.getExamDateTime().format(DATE_FORMATTER) : "Not set" %>
                    </div>
                </div>
                <div class="meta-item">
                    <div class="meta-label">Daily Study Target</div>
                    <div class="meta-value"><%= exam.getDailyStudyMinutes() %> mins/day</div>
                </div>
                <div class="meta-item">
                    <div class="meta-label">Target Marks</div>
                    <div class="meta-value">
                        <%= (exam.getTargetMarks() != null) ? escapeHtml(exam.getTargetMarks().toString()) : "Not set" %>
                    </div>
                </div>
                <div class="meta-item">
                    <div class="meta-label">Total Syllabus Effort</div>
                    <div class="meta-value">
                        <%= totalMinutes %> mins (<%= String.format("%.1f", totalMinutes / 60.0) %> hrs)
                    </div>
                </div>
            </div>
        </div>

        <!-- 2. Study Plan Section (CTA or Schedule Display) -->
        <% if (currentPlan == null) { %>
            <div class="plan-cta-card">
                <div>
                    <h2 class="plan-cta-title">Baseline Study Plan</h2>
                    <p class="plan-cta-desc">
                        Convert your syllabus topics, estimated effort, and daily study target into a realistic,
                        deterministic day-by-day study schedule before your exam date.
                    </p>
                </div>
                <form method="post" action="<%= request.getContextPath() %>/study-plan/generate">
                    <input type="hidden" name="csrfToken" value="<%= escapeHtml(request.getAttribute("csrfToken")) %>" />
                    <input type="hidden" name="examId" value="<%= exam.getId() %>" />
                    <button type="submit" class="btn-generate-plan" <%= (topics == null || topics.isEmpty()) ? "disabled title='Add syllabus topics first'" : "" %>>
                        &#9889; Generate Initial Study Plan
                    </button>
                </form>
            </div>
        <% } else { %>
            <div class="plan-section-card">
                <div class="plan-header">
                    <div class="plan-title-box">
                        <h2 class="plan-title">Initial Baseline Study Plan</h2>
                        <span class="plan-version-badge">Version <%= currentPlan.getVersion() %></span>
                    </div>
                    <div class="plan-meta-stats">
                        <span>Total Tasks: <strong><%= currentPlan.getTasks().size() %></strong></span>
                        <span>Planned Study: <strong><%= String.format("%.1f", currentPlan.getTotalPlannedMinutes() / 60.0) %> hrs</strong></span>
                        <span>Generated: <strong><%= currentPlan.getCreatedAt() != null ? currentPlan.getCreatedAt().format(DATE_FORMATTER) : "Active" %></strong></span>
                    </div>
                </div>

                <% if (currentPlan.getChangeReason() != null && currentPlan.getChangeReason().contains("shortfall")) { %>
                    <div class="alert alert-warning" style="margin-bottom: 1.25rem;">
                        <strong>&#9888; Workload Shortfall Notice:</strong>
                        The total syllabus effort exceeds the available study days before your exam date without exceeding your daily study limit.
                        High-priority and prerequisite topics have been scheduled first. Consider increasing your daily study time or prioritizing core topics.
                    </div>
                <% } %>

                <% if (currentPlan.getTasks() == null || currentPlan.getTasks().isEmpty()) { %>
                    <p style="color: var(--text-muted); font-size: 0.9rem; padding: 1rem 0;">No tasks scheduled in this plan version.</p>
                <% } else { %>
                    <table class="tasks-table">
                        <thead>
                            <tr>
                                <th>Scheduled Window</th>
                                <th>Topic</th>
                                <th>Priority</th>
                                <th>Planned Duration</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (StudyTask task : currentPlan.getTasks()) { %>
                                <tr>
                                    <td class="task-date-col">
                                        <%= task.getPlannedStart() != null ? task.getPlannedStart().format(TASK_START_FORMATTER) : "-" %>
                                        <div class="task-time-sub">
                                            until <%= task.getPlannedEnd() != null ? task.getPlannedEnd().format(TASK_TIME_FORMATTER) : "-" %>
                                        </div>
                                    </td>
                                    <td>
                                        <div class="task-topic-name">
                                            <% if (task.isTopicIsSubtopic()) { %>
                                                <span class="task-sub-arrow">&#x21B3;</span>
                                            <% } %>
                                            <%= escapeHtml(task.getTopicName()) %>
                                        </div>
                                    </td>
                                    <td>
                                        <span class="badge-priority priority-<%= escapeHtml(task.getTopicPriority()) %>">
                                            <%= "TEACHER_IMPORTANT".equals(task.getTopicPriority()) ? "Teacher Important" : escapeHtml(task.getTopicPriority()) %>
                                        </span>
                                    </td>
                                    <td>
                                        <%= task.getPlannedMinutes() %> mins
                                    </td>
                                    <td>
                                        <span class="badge-status status-<%= escapeHtml(task.getStatus()) %>">
                                            <%= escapeHtml(task.getStatus()) %>
                                        </span>
                                    </td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                <% } %>
            </div>
        <% } %>

        <!-- 3. Syllabus Topics & Topic Creation Grid -->
        <div class="workspace-grid">
            <!-- Left: Syllabus Topics List -->
            <div class="panel-card">
                <div class="panel-header">
                    <h2 class="panel-title">Syllabus Topics</h2>
                    <span style="font-size: 0.85rem; color: var(--text-muted); font-weight: 500;">
                        <%= topics != null ? topics.size() : 0 %> topic<%= (topics != null && topics.size() == 1) ? "" : "s" %>
                    </span>
                </div>

                <% if (topics == null || topics.isEmpty()) { %>
                    <div class="empty-topics">
                        <p style="margin-bottom: 0.5rem; font-weight: 500;">No topics added to syllabus yet.</p>
                        <p style="font-size: 0.825rem;">Use the form to add syllabus modules, topics, and subtopics.</p>
                    </div>
                <% } else { %>
                    <ul class="topic-list">
                        <% for (Topic topic : topics) { %>
                            <li class="topic-item <%= topic.isSubtopic() ? "subtopic" : "" %>">
                                <div>
                                    <div class="topic-name">
                                        <% if (topic.isSubtopic()) { %>
                                            <span class="topic-sub-indicator">&#x21B3; Subtopic:</span>
                                        <% } %>
                                        <%= escapeHtml(topic.getName()) %>
                                    </div>
                                    <div class="topic-est-time">
                                        Estimated effort: <%= topic.getEstimatedMinutes() %> mins
                                    </div>
                                </div>
                                <div class="topic-badges">
                                    <span class="badge-priority priority-<%= escapeHtml(topic.getPriority()) %>">
                                        <%= "TEACHER_IMPORTANT".equals(topic.getPriority()) ? "Teacher Important" : escapeHtml(topic.getPriority()) %>
                                    </span>
                                </div>
                            </li>
                        <% } %>
                    </ul>
                <% } %>
            </div>

            <!-- Right: Add Topic Form -->
            <div class="panel-card">
                <div class="panel-header">
                    <h2 class="panel-title">Add Syllabus Topic</h2>
                </div>

                <form method="post" action="<%= request.getContextPath() %>/topics/save">
                    <!-- CSRF & Exam ID -->
                    <input type="hidden" name="csrfToken" value="<%= escapeHtml(request.getAttribute("csrfToken")) %>" />
                    <input type="hidden" name="examId" value="<%= exam.getId() %>" />

                    <div class="form-group">
                        <label for="topicName">Topic / Subtopic Name</label>
                        <input type="text" id="topicName" name="name"
                               placeholder="e.g. Graph Traversal Algorithms" required />
                    </div>

                    <div class="form-group">
                        <label for="estimatedMinutes">Estimated Study Minutes</label>
                        <input type="number" id="estimatedMinutes" name="estimatedMinutes"
                               value="60" min="1" max="10000" required />
                    </div>

                    <div class="form-group">
                        <label for="priority">Study Priority</label>
                        <select id="priority" name="priority">
                            <option value="MEDIUM" selected>Medium (Standard)</option>
                            <option value="HIGH">High Priority</option>
                            <option value="TEACHER_IMPORTANT">&#9733; Teacher Important</option>
                            <option value="LOW">Low Priority</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label for="parentTopicId">Parent Topic (Optional for Subtopics)</label>
                        <select id="parentTopicId" name="parentTopicId">
                            <option value="0">None (Top-Level Topic)</option>
                            <% if (topics != null) {
                                for (Topic t : topics) {
                                    if (!t.isSubtopic()) { %>
                                        <option value="<%= t.getId() %>">Subtopic of: <%= escapeHtml(t.getName()) %></option>
                                    <% }
                                }
                            } %>
                        </select>
                    </div>

                    <button type="submit" class="btn-add-topic">+ Add Topic</button>
                </form>
            </div>
        </div>
    <% } %>
</main>

</body>
</html>
