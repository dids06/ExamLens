<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Something went wrong" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="auth">
    <div class="card center">
        <h1 style="font-size:1.5rem;">Something went wrong</h1>
        <p class="muted">The request could not be completed. Nothing was saved. Please try again.</p>
        <a class="btn btn--primary" href="${ctx}/dashboard">Back to dashboard</a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
