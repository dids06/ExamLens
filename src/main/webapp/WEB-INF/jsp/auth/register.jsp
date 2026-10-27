<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Create account" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="auth">
    <div class="auth__brand">
        <span class="brand-mark" aria-hidden="true">EL</span>
        <span>ExamLens</span>
    </div>
    <div class="card">
        <h1 style="font-size:1.35rem;">Create your account</h1>
        <p class="muted small" style="margin-top:0;">AI-first preparation: the model decides importance, order, duration and pacing.</p>

        <form method="post" action="${ctx}/auth/register" autocomplete="on">
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <div class="field">
                <label for="displayName">Your name</label>
                <input type="text" id="displayName" name="displayName" autocomplete="name" required maxlength="120">
            </div>
            <div class="field">
                <label for="email">Email address</label>
                <input type="email" id="email" name="email" autocomplete="email" required maxlength="254">
            </div>
            <div class="form-grid">
                <div class="field">
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password" autocomplete="new-password" required>
                </div>
                <div class="field">
                    <label for="confirmPassword">Confirm password</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" autocomplete="new-password" required>
                </div>
            </div>
            <div class="field">
                <label for="timezone">Time zone</label>
                <input type="text" id="timezone" name="timezone" value="UTC" maxlength="64" placeholder="e.g. Europe/London">
                <span class="help">Used for scheduling your study plan. Defaults to UTC.</span>
            </div>
            <button type="submit" class="btn btn--primary btn--block" data-working-label="Creating account…">Create account</button>
        </form>
    </div>
    <p class="auth__foot">Already have an account? <a href="${ctx}/auth/login">Sign in</a></p>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
