<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Study plan" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>Study plan</h1>
        <p class="lede">The AI decides topic priority, task order, durations and pacing around your declared availability. Every version is kept.</p>
    </div>
    <div class="page-head__actions">
        <a class="btn" href="${ctx}/plan/feasibility?examId=${exam.id}">Check feasibility</a>
        <a class="btn" href="${ctx}/plan/history?examId=${exam.id}">History</a>
        <c:if test="${not empty planView}">
            <form method="post" action="${ctx}/plan/replan"><input type="hidden" name="_csrf" value="${csrfToken}"><input type="hidden" name="examId" value="${exam.id}">
                <button type="submit" class="btn" data-working-label="Replanning…">Replan</button></form>
        </c:if>
        <form method="post" action="${ctx}/plan/generate"><input type="hidden" name="_csrf" value="${csrfToken}"><input type="hidden" name="examId" value="${exam.id}">
            <button type="submit" class="btn btn--primary" data-working-label="Generating…">${empty planView ? 'Generate plan' : 'Generate new version'}</button></form>
    </div>
</div>

<c:choose>
    <c:when test="${empty planView}">
        <div class="empty">
            <h3>No study plan yet</h3>
            <p class="muted">Confirm your syllabus topics, then generate a plan. The AI will sequence and pace the work for you.</p>
            <form method="post" action="${ctx}/plan/generate">
                <input type="hidden" name="_csrf" value="${csrfToken}">
                <input type="hidden" name="examId" value="${exam.id}">
                <button type="submit" class="btn btn--primary" data-working-label="Generating…">Generate study plan</button>
            </form>
        </div>
    </c:when>
    <c:otherwise>
        <div class="grid grid--4 section">
            <div class="card stat"><div class="stat__label">Tasks</div><div class="stat__value">${planView.completedTasks}<span class="muted" style="font-size:1rem;font-weight:600;"> / ${planView.totalTasks}</span></div><div class="stat__sub">${planView.completionPercent}% complete</div></div>
            <div class="card stat"><div class="stat__label">Planned time</div><div class="stat__value" style="font-size:1.2rem;">${planView.version.totalHoursDisplay}</div><div class="stat__sub">Across ${planView.totalTasks} tasks</div></div>
            <div class="card stat"><div class="stat__label">Feasibility</div><div class="stat__value" style="font-size:1.05rem;">${planView.version.feasibilityLabel}</div><div class="stat__sub">AI judgement</div></div>
            <div class="card stat"><div class="stat__label">Capacity</div><div class="stat__value" style="font-size:1.2rem;">${planView.availableCapacityMinutes}</div><div class="stat__sub">minutes available</div></div>
        </div>

        <div class="progress section"><div class="progress__bar" style="width:${planView.completionPercent}%;"></div></div>

        <c:if test="${not empty planView.version.summary}">
            <div class="panel panel--ai section">
                <div class="row row--between"><span class="badge badge--ai">AI plan summary</span><c:if test="${planView.version.simulated}"><span class="badge badge--warn">simulated</span></c:if></div>
                <p style="margin:.5rem 0 0;">${fn:escapeXml(planView.version.summary)}</p>
                <c:if test="${not empty planView.version.feasibilityNote}"><p class="small muted">${fn:escapeXml(planView.version.feasibilityNote)}</p></c:if>
            </div>
        </c:if>

        <section class="section">
            <div class="section__head"><h2>Scheduled tasks</h2><a class="hint" href="${ctx}/plan/tasks?examId=${exam.id}">Table view</a></div>
            <c:choose>
                <c:when test="${empty planView.days}">
                    <div class="empty"><p class="muted">The plan has no scheduled days yet.</p></div>
                </c:when>
                <c:otherwise>
                    <div class="stack">
                        <c:forEach var="day" items="${planView.days}">
                            <div class="card">
                                <div class="row row--between" style="margin-bottom:.4rem;">
                                    <strong>${day.date}</strong>
                                    <span class="badge badge--muted">${day.minutes} min</span>
                                </div>
                                <ul class="list" style="padding:0;">
                                    <c:forEach var="task" items="${day.tasks}">
                                        <li class="list__item">
                                            <div class="row row--between">
                                                <div>
                                                    <h4>${fn:escapeXml(task.title)}</h4>
                                                    <div class="row small muted">
                                                        <span>${task.typeLabel}</span>
                                                        <span>· ${task.durationDisplay}</span>
                                                        <c:if test="${not empty task.priority}"><span>· ${fn:toLowerCase(task.priority)}</span></c:if>
                                                    </div>
                                                    <c:if test="${not empty task.objective}"><div class="small muted">${fn:escapeXml(task.objective)}</div></c:if>
                                                    <c:if test="${not empty task.aiRationale}"><div class="small" style="color:var(--ai);">${fn:escapeXml(task.aiRationale)}</div></c:if>
                                                </div>
                                                <div class="nowrap">
                                                    <c:choose>
                                                        <c:when test="${task.completed}"><span class="badge badge--success">done</span></c:when>
                                                        <c:otherwise>
                                                            <form method="post" action="${ctx}/plan/task/status" style="display:inline;">
                                                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                                                <input type="hidden" name="examId" value="${exam.id}">
                                                                <input type="hidden" name="taskId" value="${task.id}">
                                                                <input type="hidden" name="status" value="COMPLETED">
                                                                <button type="submit" class="btn btn--sm">Mark done</button>
                                                            </form>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </div>
                                            </div>
                                        </li>
                                    </c:forEach>
                                </ul>
                            </div>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>

        <div class="row section">
            <form method="post" action="${ctx}/plan/last-minute"><input type="hidden" name="_csrf" value="${csrfToken}"><input type="hidden" name="examId" value="${exam.id}">
                <button type="submit" class="btn" data-working-label="Building…">Build last-minute revision plan</button></form>
        </div>

        <c:if test="${not empty explanation}">
            <section class="section">
                <div class="section__head"><h2>Why this version changed</h2></div>
                <div class="card">
                    <p>${fn:escapeXml(explanation.summary)}</p>
                    <c:if test="${not empty explanation.changeList}">
                        <ul class="list">
                            <c:forEach var="change" items="${explanation.changeList}">
                                <li class="list__item">
                                    <strong>${fn:escapeXml(change.change)}</strong>
                                    <div class="small muted">${fn:escapeXml(change.reason)}</div>
                                    <c:if test="${not empty change.evidence}"><div class="small muted">Evidence: ${fn:escapeXml(change.evidence)}</div></c:if>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:if>
                    <c:if test="${not empty explanation.actionList}">
                        <h3>Your next actions</h3>
                        <ul class="clean tick small"><c:forEach var="a" items="${explanation.actionList}"><li>${fn:escapeXml(a)}</li></c:forEach></ul>
                    </c:if>
                </div>
            </section>
        </c:if>

        <c:if test="${not empty planView.version.warningLines}">
            <div class="flash flash--info section"><div class="flash__body">
                <strong>Notes from planning:</strong>
                <ul class="clean small" style="margin:.3rem 0 0;"><c:forEach var="w" items="${planView.version.warningLines}"><li>${fn:escapeXml(w)}</li></c:forEach></ul>
            </div></div>
        </c:if>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
