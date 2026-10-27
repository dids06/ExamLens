<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Sign out" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="auth">
    <div class="card">
        <h1 style="font-size:1.25rem;">Sign out of ExamLens?</h1>
        <p class="muted small">Your data stays saved in your account.</p>
        <form method="post" action="${ctx}/auth/logout" class="form-actions">
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <button type="submit" class="btn btn--primary">Sign out</button>
            <a class="btn" href="${ctx}/dashboard">Stay signed in</a>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
