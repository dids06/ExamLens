<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.examlens.model.User" %>
<%@ page import="com.examlens.model.Exam" %>
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
%>
<%
    User currentUser = (User) request.getAttribute("currentUser");
    if (currentUser == null && session != null) {
        currentUser = (User) session.getAttribute("user");
    }

    @SuppressWarnings("unchecked")
    List<Exam> exams = (List<Exam>) request.getAttribute("exams");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard - ExamLens</title>
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
            --danger-bg: #fee2e2;
            --danger-border: #fca5a5;
            --danger-text: #dc2626;
            --success-bg: #dcfce7;
            --success-border: #bbf7d0;
            --success-text: #15803d;
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

        .nav-actions {
            display: flex;
            align-items: center;
            gap: 1.25rem;
        }

        .user-badge {
            font-size: 0.875rem;
            color: var(--text-muted);
        }

        .btn-logout {
            background-color: var(--danger-bg);
            color: var(--danger-text);
            border: 1px solid var(--danger-border);
            padding: 0.45rem 0.95rem;
            border-radius: 6px;
            font-size: 0.875rem;
            font-weight: 600;
            cursor: pointer;
            transition: background-color 0.15s ease;
        }

        .btn-logout:hover {
            background-color: #fecaca;
        }

        .container {
            max-width: 1000px;
            margin: 2rem auto;
            padding: 0 1.5rem;
        }

        .alert {
            padding: 0.875rem 1.25rem;
            border-radius: 8px;
            font-size: 0.9rem;
            margin-bottom: 1.5rem;
        }

        .alert-error {
            background-color: #fef2f2;
            border: 1px solid #fecaca;
            color: #b91c1c;
        }

        .alert-success {
            background-color: var(--success-bg);
            border: 1px solid var(--success-border);
            color: var(--success-text);
        }

        .profile-card {
            background: var(--card-bg);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 1.5rem 2rem;
            margin-bottom: 2rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 1rem;
        }

        .welcome-title {
            font-size: 1.4rem;
            font-weight: 700;
            margin-bottom: 0.25rem;
        }

        .welcome-desc {
            color: var(--text-muted);
            font-size: 0.9rem;
        }

        .section-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 1.25rem;
        }

        .section-title {
            font-size: 1.25rem;
            font-weight: 700;
        }

        .btn-primary {
            display: inline-flex;
            align-items: center;
            gap: 0.35rem;
            background-color: var(--primary);
            color: white;
            text-decoration: none;
            padding: 0.6rem 1.15rem;
            border-radius: 6px;
            font-size: 0.9rem;
            font-weight: 600;
            transition: background-color 0.15s ease;
        }

        .btn-primary:hover {
            background-color: var(--primary-hover);
        }

        .exam-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
            gap: 1.25rem;
        }

        .exam-card {
            background: var(--card-bg);
            border: 1px solid var(--border);
            border-radius: 10px;
            padding: 1.5rem;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            transition: transform 0.1s ease, box-shadow 0.1s ease;
        }

        .exam-card:hover {
            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
            border-color: #cbd5e1;
        }

        .exam-subject {
            font-size: 1.15rem;
            font-weight: 700;
            margin-bottom: 0.75rem;
            color: var(--text);
        }

        .exam-details {
            display: flex;
            flex-direction: column;
            gap: 0.4rem;
            font-size: 0.875rem;
            color: var(--text-muted);
            margin-bottom: 1.25rem;
        }

        .exam-detail-item {
            display: flex;
            justify-content: space-between;
        }

        .exam-detail-label {
            color: var(--text-muted);
        }

        .exam-detail-value {
            font-weight: 600;
            color: var(--text);
        }

        .badge-status {
            display: inline-block;
            background-color: #e0f2fe;
            color: #0369a1;
            font-size: 0.75rem;
            font-weight: 600;
            padding: 0.2rem 0.5rem;
            border-radius: 4px;
            text-transform: uppercase;
        }

        .btn-view-exam {
            display: block;
            text-align: center;
            background-color: #f1f5f9;
            color: var(--primary);
            text-decoration: none;
            padding: 0.55rem;
            border-radius: 6px;
            font-size: 0.875rem;
            font-weight: 600;
            border: 1px solid var(--border);
            transition: background-color 0.15s ease;
        }

        .btn-view-exam:hover {
            background-color: #e2e8f0;
        }

        .empty-state {
            background: var(--card-bg);
            border: 2px dashed var(--border);
            border-radius: 12px;
            padding: 3rem 2rem;
            text-align: center;
        }

        .empty-state-title {
            font-size: 1.15rem;
            font-weight: 600;
            margin-bottom: 0.5rem;
        }

        .empty-state-desc {
            color: var(--text-muted);
            font-size: 0.9rem;
            max-width: 480px;
            margin: 0 auto 1.5rem auto;
            line-height: 1.5;
        }
    </style>
</head>
<body>

<header class="navbar">
    <a href="<%= request.getContextPath() %>/dashboard" class="brand">ExamLens</a>
    <div class="nav-actions">
        <span class="user-badge"><%= currentUser != null ? escapeHtml(currentUser.getEmail()) : "" %></span>
        <form method="post" action="<%= request.getContextPath() %>/logout" style="display:inline;">
            <input type="hidden" name="csrfToken" value="<%= escapeHtml(request.getAttribute("csrfToken")) %>" />
            <button type="submit" class="btn-logout">Log Out</button>
        </form>
    </div>
</header>

<main class="container">
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

    <div class="profile-card">
        <div>
            <h1 class="welcome-title">Welcome back, <%= currentUser != null ? escapeHtml(currentUser.getName()) : "Student" %>!</h1>
            <p class="welcome-desc">Student ID: #<%= currentUser != null && currentUser.getId() != null ? escapeHtml(currentUser.getId()) : "-" %> &bull; <%= currentUser != null ? escapeHtml(currentUser.getEmail()) : "" %></p>
        </div>
        <a href="<%= request.getContextPath() %>/exams/create" class="btn-primary">+ Create Exam Workspace</a>
    </div>

    <div class="section-header">
        <h2 class="section-title">Your Exam Workspaces</h2>
        <span style="font-size: 0.875rem; color: var(--text-muted); font-weight: 500;">
            <%= (exams != null) ? exams.size() : 0 %> workspace<%= (exams != null && exams.size() == 1) ? "" : "s" %>
        </span>
    </div>

    <% if (exams == null || exams.isEmpty()) { %>
        <div class="empty-state">
            <h3 class="empty-state-title">No Exam Workspaces Yet</h3>
            <p class="empty-state-desc">
                Set up your first exam workspace to organize your syllabus topics, define your target exam date, and allocate daily study time.
            </p>
            <a href="<%= request.getContextPath() %>/exams/create" class="btn-primary">+ Create Your First Exam</a>
        </div>
    <% } else { %>
        <div class="exam-grid">
            <% for (Exam exam : exams) { %>
                <div class="exam-card">
                    <div>
                        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.5rem;">
                            <h3 class="exam-subject"><%= escapeHtml(exam.getSubjectName()) %></h3>
                            <span class="badge-status"><%= escapeHtml(exam.getStatus()) %></span>
                        </div>
                        <div class="exam-details">
                            <div class="exam-detail-item">
                                <span class="exam-detail-label">Exam Date:</span>
                                <span class="exam-detail-value">
                                    <%= exam.getExamDateTime() != null ? exam.getExamDateTime().format(DATE_FORMATTER) : "Not set" %>
                                </span>
                            </div>
                            <div class="exam-detail-item">
                                <span class="exam-detail-label">Daily Study:</span>
                                <span class="exam-detail-value"><%= exam.getDailyStudyMinutes() %> mins/day</span>
                            </div>
                            <div class="exam-detail-item">
                                <span class="exam-detail-label">Target Marks:</span>
                                <span class="exam-detail-value">
                                    <%= (exam.getTargetMarks() != null) ? escapeHtml(exam.getTargetMarks().toString()) : "Optional" %>
                                </span>
                            </div>
                        </div>
                    </div>
                    <a href="<%= request.getContextPath() %>/exams/view?id=<%= exam.getId() %>" class="btn-view-exam">
                        Open Workspace &rarr;
                    </a>
                </div>
            <% } %>
        </div>
    <% } %>
</main>

</body>
</html>
