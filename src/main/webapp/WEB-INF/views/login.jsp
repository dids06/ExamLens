<%@ page contentType="text/html;charset=UTF-8" language="java" %>
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
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign In - ExamLens</title>
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
            --success-bg: #f0fdf4;
            --success-border: #bbf7d0;
            --success-text: #15803d;
            --info-bg: #eff6ff;
            --info-border: #bfdbfe;
            --info-text: #1d4ed8;
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
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 1.5rem;
        }

        .auth-card {
            background: var(--card-bg);
            width: 100%;
            max-width: 420px;
            padding: 2.25rem;
            border-radius: 12px;
            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
            border: 1px solid var(--border);
        }

        .logo-area {
            text-align: center;
            margin-bottom: 1.5rem;
        }

        .logo-title {
            font-size: 1.75rem;
            font-weight: 700;
            color: var(--primary);
            letter-spacing: -0.025em;
        }

        .logo-subtitle {
            font-size: 0.875rem;
            color: var(--text-muted);
            margin-top: 0.25rem;
        }

        .alert {
            padding: 0.75rem 1rem;
            border-radius: 8px;
            font-size: 0.875rem;
            margin-bottom: 1.25rem;
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

        .alert-info {
            background-color: var(--info-bg);
            border: 1px solid var(--info-border);
            color: var(--info-text);
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

        input[type="email"],
        input[type="password"] {
            width: 100%;
            padding: 0.625rem 0.875rem;
            font-size: 0.95rem;
            border: 1px solid var(--border);
            border-radius: 6px;
            background-color: #fff;
            color: var(--text);
            outline: none;
            transition: border-color 0.15s ease, box-shadow 0.15s ease;
        }

        input[type="email"]:focus,
        input[type="password"]:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
        }

        .btn-submit {
            width: 100%;
            padding: 0.75rem;
            background-color: var(--primary);
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 0.95rem;
            font-weight: 600;
            cursor: pointer;
            transition: background-color 0.15s ease;
            margin-top: 0.5rem;
        }

        .btn-submit:hover {
            background-color: var(--primary-hover);
        }

        .auth-footer {
            margin-top: 1.5rem;
            text-align: center;
            font-size: 0.875rem;
            color: var(--text-muted);
        }

        .auth-footer a {
            color: var(--primary);
            text-decoration: none;
            font-weight: 500;
        }

        .auth-footer a:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>

<div class="auth-card">
    <div class="logo-area">
        <h1 class="logo-title">ExamLens</h1>
        <p class="logo-subtitle">Adaptive Exam Preparation Platform</p>
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

    <% if (request.getAttribute("infoMessage") != null) { %>
        <div class="alert alert-info">
            <%= escapeHtml(request.getAttribute("infoMessage")) %>
        </div>
    <% } %>

    <form method="post" action="<%= request.getContextPath() %>/login">
        <!-- CSRF Token Protection -->
        <input type="hidden" name="csrfToken" value="<%= escapeHtml(request.getAttribute("csrfToken")) %>" />

        <div class="form-group">
            <label for="email">Email Address</label>
            <input type="email" id="email" name="email"
                   value="<%= escapeHtml(request.getAttribute("email")) %>"
                   placeholder="student@example.com" required autofocus autocomplete="username" />
        </div>

        <div class="form-group">
            <label for="password">Password</label>
            <input type="password" id="password" name="password"
                   placeholder="••••••••" required autocomplete="current-password" />
        </div>

        <button type="submit" class="btn-submit">Sign In</button>
    </form>

    <div class="auth-footer">
        Don't have an account?
        <a href="<%= request.getContextPath() %>/register">Create student account</a>
    </div>
</div>

</body>
</html>
