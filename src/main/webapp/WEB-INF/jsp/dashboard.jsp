<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Dashboard" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<c:choose>
<c:when test="${empty activeExam}">
    <div class="page-head">
        <div class="page-head__text">
            <h1>Welcome to ExamLens</h1>
            <p class="lede">An AI-first study companion. Create an exam, add your sources, and let the model decide what matters most and how to pace it.</p>
        </div>
    </div>
    <div class="empty">
        <h3>No active exam yet</h3>
        <p class="muted">Start by creating the exam you are preparing for.</p>
        <a class="btn btn--primary" href="${ctx}/exams/new">Create your first exam</a>
    </div>
</c:when>
<c:otherwise>
    <c:set var="taskPercent" value="${taskTotal > 0 ? (taskDone * 100 / taskTotal) : 0}" />
    <div class="page-head">
        <div class="page-head__text">
            <h1>${fn:escapeXml(activeExam.subjectName)}</h1>
            <p class="lede">
                <c:if test="${not empty activeExam.examDatetime}">
                    Exam on ${activeExam.examDatetimeDisplay}
                    <c:if test="${not empty activeExam.daysUntil}">
                        · <strong>${activeExam.daysUntil} day<c:if test="${activeExam.daysUntil ne 1}">s</c:if> to go</strong>
                    </c:if>
                </c:if>
                <c:if test="${empty activeExam.examDatetime}">No exam date set yet.</c:if>
            </p>
        </div>
        <div class="page-head__actions">
            <a class="btn" href="${ctx}/exams/view?id=${activeExam.id}">Exam overview</a>
            <a class="btn btn--primary" href="${ctx}/plan">${hasPlan ? 'Open study plan' : 'Build study plan'}</a>
        </div>
    </div>

    <div class="grid grid--4 section">
        <div class="card stat">
            <div class="stat__label">Syllabus topics</div>
            <div class="stat__value">${topicCount}</div>
            <div class="stat__sub">Confirmed and driving your plan</div>
        </div>
        <div class="card stat">
            <div class="stat__label">Tasks complete</div>
            <div class="stat__value">${taskDone}<span class="muted" style="font-size:1rem;font-weight:600;"> / ${taskTotal}</span></div>
            <div class="stat__sub">${taskPercent}% of the current plan</div>
        </div>
        <div class="card stat">
            <div class="stat__label">Study minutes</div>
            <div class="stat__value">${actualMinutes}</div>
            <div class="stat__sub">of ${plannedMinutes} minutes planned</div>
        </div>
        <div class="card stat">
            <div class="stat__label">Practice average</div>
            <div class="stat__value">
                <c:choose><c:when test="${empty practiceAverage}">—</c:when><c:otherwise>${practiceAverage}%</c:otherwise></c:choose>
            </div>
            <div class="stat__sub">${paperCount} paper<c:if test="${paperCount ne 1}">s</c:if> generated</div>
        </div>
    </div>

    <div class="split">
        <div class="stack">
            <section class="section">
                <div class="section__head">
                    <h2>Upcoming study tasks</h2>
                    <a class="hint" href="${ctx}/plan/tasks">View all tasks</a>
                </div>
                <c:choose>
                    <c:when test="${empty upcomingTasks}">
                        <div class="empty">
                            <h3>${hasPlan ? 'Nothing scheduled next' : 'No study plan yet'}</h3>
                            <p class="muted">${hasPlan ? 'All scheduled tasks are complete, or the plan needs a refresh.' : 'The AI will build an ordered, paced plan from your confirmed topics and availability.'}</p>
                            <a class="btn btn--primary" href="${ctx}/plan">${hasPlan ? 'Open study plan' : 'Build study plan'}</a>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="card card--flush">
                            <ul class="list" style="padding:0 1.2rem;">
                                <c:forEach var="task" items="${upcomingTasks}">
                                    <li class="list__item">
                                        <div class="row row--between">
                                            <h4>${fn:escapeXml(task.title)}</h4>
                                            <span class="badge badge--muted">${task.durationDisplay}</span>
                                        </div>
                                        <div class="row small muted">
                                            <span>${task.typeLabel}</span>
                                            <c:if test="${not empty task.scheduledDate}"><span>· ${task.scheduledDate}</span></c:if>
                                            <c:if test="${not empty task.priority}"><span>· ${fn:toLowerCase(task.priority)} priority</span></c:if>
                                        </div>
                                    </li>
                                </c:forEach>
                            </ul>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>

            <c:if test="${not empty readiness}">
                <section class="section">
                    <div class="section__head">
                        <h2>Latest readiness check</h2>
                        <a class="hint" href="${ctx}/readiness">Open readiness</a>
                    </div>
                    <div class="card">
                        <div class="row row--between" style="margin-bottom:.5rem;">
                            <span class="badge badge--ai">AI assessment</span>
                            <c:if test="${readiness.simulated}"><span class="badge badge--warn">simulated</span></c:if>
                        </div>
                        <p style="margin:.2rem 0 .6rem;">${fn:escapeXml(readiness.readinessSummary)}</p>
                        <div class="row small muted">
                            <span>Completion ${readiness.completionDisplay}</span>
                            <span>· Practice ${readiness.practiceAverageDisplay}</span>
                            <span>· ${readiness.actualMinutes} / ${readiness.plannedMinutes} min</span>
                        </div>
                    </div>
                </section>
            </c:if>
        </div>

        <div class="stack">
            <section class="section">
                <div class="section__head"><h2>Your sources</h2></div>
                <div class="card">
                    <dl class="kv">
                        <dt>Documents</dt><dd>${documentCount}</dd>
                        <dt>Revision sets</dt><dd>${revisionCount}</dd>
                        <dt>Topics</dt><dd>${topicCount}</dd>
                    </dl>
                    <div class="form-actions">
                        <a class="btn btn--sm" href="${ctx}/documents">Upload</a>
                        <a class="btn btn--sm" href="${ctx}/syllabus">Syllabus</a>
                    </div>
                </div>
            </section>

            <c:if test="${not empty latestGuidance}">
                <section class="section">
                    <div class="section__head"><h2>Teacher guidance</h2></div>
                    <div class="card">
                        <c:if test="${latestGuidance.confirmed}"><span class="badge badge--success">confirmed</span></c:if>
                        <c:if test="${not latestGuidance.confirmed}"><span class="badge badge--warn">draft</span></c:if>
                        <ul class="clean tick small" style="margin-top:.5rem;">
                            <c:forEach var="hint" items="${latestGuidance.hintLines}" varStatus="s">
                                <c:if test="${s.index < 4}"><li>${fn:escapeXml(hint)}</li></c:if>
                            </c:forEach>
                        </ul>
                        <c:if test="${empty latestGuidance.hintLines}">
                            <p class="muted small">No hints extracted yet.</p>
                        </c:if>
                        <a class="btn btn--sm" href="${ctx}/guidance" style="margin-top:.5rem;">Open guidance</a>
                    </div>
                </section>
            </c:if>

            <section class="section">
                <div class="section__head"><h2>Quick actions</h2></div>
                <div class="card stack">
                    <a class="btn btn--sm" href="${ctx}/practice">Generate a practice paper</a>
                    <a class="btn btn--sm" href="${ctx}/revision">Create revision resources</a>
                    <a class="btn btn--sm" href="${ctx}/sessions">Log a study session</a>
                    <a class="btn btn--sm" href="${ctx}/assistant">Ask the AI assistant</a>
                </div>
            </section>
        </div>
    </div>
</c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
