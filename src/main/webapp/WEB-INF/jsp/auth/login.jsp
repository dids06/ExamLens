<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Sign in" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="auth">
    <div class="auth__brand">
        <span class="brand-mark" aria-hidden="true">EL</span>
        <span>ExamLens</span>
    </div>
    <div class="card">
        <h1 style="font-size:1.35rem;">Welcome back</h1>
        <p class="muted small" style="margin-top:0;">Sign in to continue your adaptive exam preparation.</p>

        <c:if test="${reason eq 'required'}">
            <div class="flash flash--info" role="status">
                <span aria-hidden="true">i</span>
                <div class="flash__body">Please sign in to open that page.</div>
            </div>
        </c:if>

        <form method="post" action="${ctx}/auth/login" autocomplete="on">
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <div class="field">
                <label for="email">Email address</label>
                <input type="email" id="email" name="email" autocomplete="email" required maxlength="254">
            </div>
            <div class="field">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" autocomplete="current-password" required>
            </div>
            <button type="submit" class="btn btn--primary btn--block" data-working-label="Signing in…">Sign in</button>
        </form>
    </div>
    <p class="auth__foot">New to ExamLens? <a href="${ctx}/auth/register">Create an account</a></p>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
