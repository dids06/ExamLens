<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Plan history" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>Plan history</h1>
        <p class="lede">Every plan version is preserved with the AI's reasoning, so you can see how the strategy evolved.</p>
    </div>
    <div class="page-head__actions"><a class="btn" href="${ctx}/plan?examId=${exam.id}">Plan overview</a></div>
</div>

<c:choose>
    <c:when test="${empty versions}">
        <div class="empty"><h3>No versions yet</h3><p class="muted">Generate a plan to start the history.</p></div>
    </c:when>
    <c:otherwise>
        <div class="stack">
            <c:forEach var="v" items="${versions}">
                <div class="card">
                    <div class="row row--between">
                        <h3>Version ${v.versionNumber} <span class="badge badge--muted">${fn:toLowerCase(v.kind)}</span></h3>
                        <span class="row">
                            <span class="badge badge--primary">${v.feasibilityLabel}</span>
                            <c:if test="${v.simulated}"><span class="badge badge--warn">simulated</span></c:if>
                            <c:choose>
                                <c:when test="${v.status eq 'ACTIVE'}"><span class="badge badge--success">active</span></c:when>
                                <c:otherwise><span class="badge badge--muted">${fn:toLowerCase(v.status)}</span></c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="small muted">Created ${v.createdAt} · ${v.totalHoursDisplay} · capacity ${empty v.availableCapacityMinutes ? '—' : v.availableCapacityMinutes} min</div>
                    <c:if test="${not empty v.summary}"><p style="margin-top:.5rem;">${fn:escapeXml(v.summary)}</p></c:if>
                    <c:if test="${not empty v.changeSummary}"><p class="small"><strong>Change:</strong> ${fn:escapeXml(v.changeSummary)}</p></c:if>
                    <c:if test="${not empty v.feasibilityNote}"><p class="small muted">${fn:escapeXml(v.feasibilityNote)}</p></c:if>
                </div>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
