<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Page not found" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="auth">
    <div class="card center">
        <h1 style="font-size:2rem;margin-bottom:.2rem;">404</h1>
        <p class="muted">We could not find that page.</p>
        <a class="btn btn--primary" href="${ctx}/dashboard">Back to dashboard</a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
