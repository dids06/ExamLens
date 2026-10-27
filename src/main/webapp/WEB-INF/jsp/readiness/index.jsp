<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Readiness" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>Readiness</h1>
        <p class="lede">A grounded read on how prepared you are. The percentages below are computed by ExamLens from your recorded activity; the AI only interprets them.</p>
    </div>
    <div class="page-head__actions">
        <form method="post" action="${ctx}/readiness/analyse">
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <input type="hidden" name="examId" value="${exam.id}">
            <button type="submit" class="btn btn--primary" data-working-label="Analysing…">Run readiness analysis</button>
        </form>
    </div>
</div>

<div class="grid grid--4 section">
    <div class="card stat"><div class="stat__label">Task completion</div>
        <div class="stat__value">${taskTotal > 0 ? (taskDone * 100 / taskTotal) : 0}%</div>
        <div class="stat__sub">${taskDone} of ${taskTotal} tasks</div></div>
    <div class="card stat"><div class="stat__label">Planned minutes</div><div class="stat__value">${plannedMinutes}</div><div class="stat__sub">scheduled</div></div>
    <div class="card stat"><div class="stat__label">Actual minutes</div><div class="stat__value">${actualMinutes}</div><div class="stat__sub">logged</div></div>
    <div class="card stat"><div class="stat__label">Practice average</div>
        <div class="stat__value"><c:choose><c:when test="${empty practiceAverage}">—</c:when><c:otherwise>${practiceAverage}%</c:otherwise></c:choose></div>
        <div class="stat__sub">${paperCount} paper<c:if test="${paperCount ne 1}">s</c:if></div></div>
</div>

<c:choose>
    <c:when test="${empty assessment}">
        <div class="empty"><h3>No assessment yet</h3>
            <p class="muted">Run the analysis to get an honest readiness assessment grounded in your activity.</p></div>
    </c:when>
    <c:otherwise>
        <div class="panel panel--ai section">
            <div class="row row--between">
                <span class="badge badge--ai">AI assessment</span>
                <span class="row">
                    <span class="small muted">${assessment.createdAt}</span>
                    <c:if test="${assessment.simulated}"><span class="badge badge--warn">simulated</span></c:if>
                </span>
            </div>
            <p style="margin:.6rem 0 0;">${fn:escapeXml(assessment.readinessSummary)}</p>
        </div>

        <div class="split section">
            <div class="stack">
                <section class="section">
                    <div class="section__head"><h2>Weak topics</h2></div>
                    <c:choose>
                        <c:when test="${empty assessment.weakTopicList}"><div class="card"><p class="muted small">No weak topics identified.</p></div></c:when>
                        <c:otherwise>
                            <div class="stack">
                                <c:forEach var="t" items="${assessment.weakTopicList}">
                                    <div class="card">
                                        <div class="row row--between">
                                            <h3>${fn:escapeXml(t.topic)}</h3>
                                            <c:if test="${not empty t.confidence}"><span class="badge badge--muted">${fn:toLowerCase(t.confidence)} confidence</span></c:if>
                                        </div>
                                        <c:if test="${not empty t.evidence}">
                                            <ul class="clean small" style="margin-top:.3rem;"><c:forEach var="e" items="${t.evidence}"><li>· ${fn:escapeXml(e)}</li></c:forEach></ul>
                                        </c:if>
                                        <c:if test="${not empty t.recommendedNextAction}">
                                            <p class="small" style="color:var(--ai);margin:.4rem 0 0;">Next: ${fn:escapeXml(t.recommendedNextAction)}</p>
                                        </c:if>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>
            </div>
            <div class="stack">
                <c:if test="${not empty assessment.strengthList}">
                    <section class="section">
                        <div class="section__head"><h2>Strengths</h2></div>
                        <div class="card"><ul class="clean tick small"><c:forEach var="s" items="${assessment.strengthList}"><li>${fn:escapeXml(s)}</li></c:forEach></ul></div>
                    </section>
                </c:if>
                <c:if test="${not empty assessment.gapList}">
                    <section class="section">
                        <div class="section__head"><h2>Coverage gaps</h2></div>
                        <div class="card"><ul class="clean cross small"><c:forEach var="g" items="${assessment.gapList}"><li>${fn:escapeXml(g)}</li></c:forEach></ul></div>
                    </section>
                </c:if>
                <c:if test="${not empty assessment.limitationList}">
                    <section class="section">
                        <div class="section__head"><h2>Limitations</h2></div>
                        <div class="card"><ul class="clean small"><c:forEach var="l" items="${assessment.limitationList}"><li class="muted">${fn:escapeXml(l)}</li></c:forEach></ul></div>
                    </section>
                </c:if>
            </div>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
