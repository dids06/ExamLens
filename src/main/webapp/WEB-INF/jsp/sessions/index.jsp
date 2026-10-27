<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Study sessions" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>Study sessions</h1>
        <p class="lede">Record what you actually did. The AI turns those facts into feedback — it never invents your activity.</p>
    </div>
</div>

<div class="grid grid--3 section">
    <div class="card stat"><div class="stat__label">Sessions logged</div><div class="stat__value">${fn:length(sessions)}</div><div class="stat__sub">recorded</div></div>
    <div class="card stat"><div class="stat__label">Actual minutes</div><div class="stat__value">${actualMinutes}</div><div class="stat__sub">total logged</div></div>
    <div class="card stat"><div class="stat__label">Planned minutes</div><div class="stat__value">${plannedMinutes}</div><div class="stat__sub">scheduled</div></div>
</div>

<div class="split">
    <section class="section">
        <div class="section__head"><h2>Session log</h2></div>
        <c:choose>
            <c:when test="${empty sessions}">
                <div class="empty"><h3>No sessions logged</h3><p class="muted">Log your first study session to start building evidence.</p></div>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="table">
                        <thead><tr><th>Date</th><th>Task</th><th class="num">Planned</th><th class="num">Actual</th><th>Feel</th><th>Status</th></tr></thead>
                        <tbody>
                            <c:forEach var="s" items="${sessions}">
                                <tr>
                                    <td class="nowrap">${s.sessionDate}</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty s.taskId}">Task #${s.taskId}</c:when>
                                            <c:otherwise><span class="muted">Free study</span></c:otherwise>
                                        </c:choose>
                                        <c:if test="${not empty s.studentNotes}"><div class="small muted truncate" style="max-width:240px;">${fn:escapeXml(s.studentNotes)}</div></c:if>
                                    </td>
                                    <td class="num">${empty s.plannedMinutes ? '—' : s.plannedMinutes}</td>
                                    <td class="num">${s.actualMinutes}</td>
                                    <td>${empty s.difficultyFeedback ? '—' : fn:replace(fn:toLowerCase(s.difficultyFeedback), '_', ' ')}</td>
                                    <td><span class="badge ${s.status eq 'COMPLETED' ? 'badge--success' : 'badge--muted'}">${fn:toLowerCase(s.status)}</span></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </section>

    <section class="section">
        <div class="section__head"><h2>Log a session</h2></div>
        <div class="card">
            <form method="post" action="${ctx}/sessions/log">
                <input type="hidden" name="_csrf" value="${csrfToken}">
                <input type="hidden" name="examId" value="${exam.id}">
                <div class="field">
                    <label for="taskId">Study task (optional)</label>
                    <select id="taskId" name="taskId">
                        <option value="">Free study (no task)</option>
                        <c:forEach var="task" items="${tasks}">
                            <option value="${task.id}">${fn:escapeXml(task.title)} · ${task.durationDisplay}</option>
                        </c:forEach>
                    </select>
                </div>
                <div class="form-grid">
                    <div class="field"><label for="actualMinutes">Actual minutes</label><input type="number" id="actualMinutes" name="actualMinutes" min="0" max="1440" value="60" required></div>
                    <div class="field"><label for="plannedMinutes">Planned minutes</label><input type="number" id="plannedMinutes" name="plannedMinutes" min="0" max="1440" placeholder="optional"></div>
                </div>
                <div class="field">
                    <label for="difficultyFeedback">How did it feel?</label>
                    <select id="difficultyFeedback" name="difficultyFeedback">
                        <option value="">No feedback</option>
                        <option value="EASIER">Easier than expected</option>
                        <option value="AS_EXPECTED">As expected</option>
                        <option value="HARDER">Harder than expected</option>
                    </select>
                </div>
                <div class="field">
                    <label for="notes">Notes</label>
                    <textarea id="notes" name="notes" maxlength="2000" placeholder="What did you cover? What was tricky?"></textarea>
                </div>
                <label class="check"><input type="checkbox" name="markComplete" value="true"> Mark the linked task complete</label>
                <div class="form-actions">
                    <button type="submit" class="btn btn--primary" data-working-label="Saving…">Log session</button>
                </div>
            </form>
        </div>
    </section>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
