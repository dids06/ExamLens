<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.examlens.model.User" %>
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
%>
<%
    User currentUser = (User) session.getAttribute("user");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create Exam Workspace - ExamLens</title>
    <style>
        :root {
            --primary: #2563eb;
            --primary-hover: #1d4ed8;
            --bg: #f8fafc;
            --card-bg: #ffffff;
            --text: #0f172a;
            --text-muted: #64748b;
            --border: #e2e8f0;
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
            max-width: 620px;
            margin: 2.5rem auto;
            padding: 0 1.5rem;
        }

        .form-card {
            background: var(--card-bg);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 2.25rem;
            box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
        }

        .form-header {
            margin-bottom: 1.5rem;
        }

        .form-title {
            font-size: 1.4rem;
            font-weight: 700;
            margin-bottom: 0.35rem;
        }

        .form-subtitle {
            color: var(--text-muted);
            font-size: 0.875rem;
            line-height: 1.4;
        }

        .alert-error {
            background-color: var(--error-bg);
            border: 1px solid var(--error-border);
            color: var(--error-text);
            padding: 0.75rem 1rem;
            border-radius: 8px;
            font-size: 0.875rem;
            margin-bottom: 1.25rem;
        }

        .form-group {
            margin-bottom: 1.25rem;
        }

        label {
            display: block;
            font-size: 0.875rem;
            font-weight: 500;
            margin-bottom: 0.375rem;
            color: var(--text);
        }

        .label-optional {
            font-weight: 400;
            color: var(--text-muted);
            font-size: 0.8rem;
        }

        input[type="text"],
        input[type="datetime-local"],
        input[type="number"] {
            width: 100%;
            padding: 0.65rem 0.875rem;
            font-size: 0.95rem;
            border: 1px solid var(--border);
            border-radius: 6px;
            background-color: #fff;
            color: var(--text);
            outline: none;
            transition: border-color 0.15s ease, box-shadow 0.15s ease;
        }

        input[type="text"]:focus,
        input[type="datetime-local"]:focus,
        input[type="number"]:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
        }

        .form-hint {
            font-size: 0.75rem;
            color: var(--text-muted);
            margin-top: 0.25rem;
        }

        .form-actions {
            display: flex;
            align-items: center;
            justify-content: flex-end;
            gap: 1rem;
            margin-top: 1.75rem;
            padding-top: 1.25rem;
            border-top: 1px solid var(--border);
        }

        .btn-cancel {
            color: var(--text-muted);
            text-decoration: none;
            font-size: 0.9rem;
            font-weight: 500;
            padding: 0.65rem 1rem;
        }

        .btn-cancel:hover {
            color: var(--text);
        }

        .btn-submit {
            padding: 0.65rem 1.25rem;
            background-color: var(--primary);
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 0.9rem;
            font-weight: 600;
            cursor: pointer;
            transition: background-color 0.15s ease;
        }

        .btn-submit:hover {
            background-color: var(--primary-hover);
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
    <div class="form-card">
        <div class="form-header">
            <h1 class="form-title">Create Exam Workspace</h1>
            <p class="form-subtitle">
                Set up a new workspace with your exam subject, exam deadline, and available daily study time.
            </p>
        </div>

        <% if (request.getAttribute("errorMessage") != null) { %>
            <div class="alert-error">
                <%= escapeHtml(request.getAttribute("errorMessage")) %>
            </div>
        <% } %>

        <form method="post" action="<%= request.getContextPath() %>/exams/create">
            <!-- CSRF Protection -->
            <input type="hidden" name="csrfToken" value="<%= escapeHtml(request.getAttribute("csrfToken")) %>" />

            <div class="form-group">
                <label for="subjectName">Subject Name</label>
                <input type="text" id="subjectName" name="subjectName"
                       value="<%= escapeHtml(request.getAttribute("subjectName")) %>"
                       placeholder="e.g. Data Structures and Algorithms" required autofocus />
                <div class="form-hint">Between 2 and 150 characters.</div>
            </div>

            <div class="form-group">
                <label for="examDateTime">Exam Date and Time</label>
                <input type="datetime-local" id="examDateTime" name="examDateTime"
                       value="<%= escapeHtml(request.getAttribute("examDateTime")) %>" required />
                <div class="form-hint">Must be scheduled in the future.</div>
            </div>

            <div class="form-group">
                <label for="dailyStudyMinutes">Daily Study Target (Minutes)</label>
                <input type="number" id="dailyStudyMinutes" name="dailyStudyMinutes"
                       value="<%= request.getAttribute("dailyStudyMinutes") != null ? escapeHtml(request.getAttribute("dailyStudyMinutes")) : "120" %>"
                       min="1" max="1440" required />
                <div class="form-hint">How many minutes per day can you dedicate to this subject? (e.g. 120 = 2 hours)</div>
            </div>

            <div class="form-group">
                <label for="targetMarks">Target Marks <span class="label-optional">(Optional)</span></label>
                <input type="number" id="targetMarks" name="targetMarks"
                       value="<%= escapeHtml(request.getAttribute("targetMarks")) %>"
                       min="0" max="1000" step="0.5" placeholder="e.g. 85.0" />
                <div class="form-hint">Your target score or grade goal for this exam.</div>
            </div>

            <div class="form-actions">
                <a href="<%= request.getContextPath() %>/dashboard" class="btn-cancel">Cancel</a>
                <button type="submit" class="btn-submit">Create Exam Workspace</button>
            </div>
        </form>
    </div>
</main>

</body>
</html>
