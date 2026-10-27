<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Exam overview" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>${fn:escapeXml(exam.subjectName)}</h1>
        <p class="lede">
            <c:choose>
                <c:when test="${not empty exam.examDatetime}">${exam.examDatetimeDisplay}
                    <c:if test="${not empty exam.daysUntil}"> · ${exam.daysUntil} day<c:if test="${exam.daysUntil ne 1}">s</c:if> to go</c:if>
                </c:when>
                <c:otherwise>No exam date set.</c:otherwise>
            </c:choose>
        </p>
    </div>
    <div class="page-head__actions">
        <a class="btn" href="${ctx}/exams/availability?examId=${exam.id}">Study availability</a>
        <a class="btn btn--primary" href="${ctx}/plan?examId=${exam.id}">${hasPlan ? 'Study plan' : 'Build plan'}</a>
    </div>
</div>

<div class="grid grid--4 section">
    <div class="card stat"><div class="stat__label">Topics</div><div class="stat__value">${topicCount}</div><div class="stat__sub">Confirmed</div></div>
    <div class="card stat"><div class="stat__label">Documents</div><div class="stat__value">${documentCount}</div><div class="stat__sub">Uploaded sources</div></div>
    <div class="card stat"><div class="stat__label">Plan</div><div class="stat__value" style="font-size:1.2rem;">${hasPlan ? 'Active' : 'None'}</div><div class="stat__sub">${hasPlan ? 'Tasks scheduled' : 'Generate one'}</div></div>
    <div class="card stat">
        <div class="stat__label">Marks</div>
        <div class="stat__value" style="font-size:1.2rem;">
            <c:choose><c:when test="${not empty exam.totalMarks}">${exam.totalMarks}</c:when><c:otherwise>—</c:otherwise></c:choose>
        </div>
        <div class="stat__sub">Target <c:choose><c:when test="${not empty exam.targetMarks}">${exam.targetMarks}</c:when><c:otherwise>—</c:otherwise></c:choose></div>
    </div>
</div>

<div class="split">
    <div class="stack">
        <section class="section">
            <div class="section__head"><h2>Workflow</h2><span class="hint">Prepare each step in order</span></div>
            <div class="grid grid--2">
                <a class="card card-link" href="${ctx}/documents?examId=${exam.id}"><h3>1 · Documents</h3><p class="muted small">Upload syllabus, notes, materials and past papers.</p></a>
                <a class="card card-link" href="${ctx}/syllabus?examId=${exam.id}"><h3>2 · Syllabus</h3><p class="muted small">AI extracts and classifies topics for you to confirm.</p></a>
                <a class="card card-link" href="${ctx}/guidance?examId=${exam.id}"><h3>3 · Teacher guidance</h3><p class="muted small">Hints, instructions and important topics.</p></a>
                <a class="card card-link" href="${ctx}/materials?examId=${exam.id}"><h3>4 · Materials</h3><p class="muted small">Index sources and compare coverage.</p></a>
                <a class="card card-link" href="${ctx}/pyq?examId=${exam.id}"><h3>5 · Previous papers</h3><p class="muted small">Analyse recurring patterns honestly.</p></a>
                <a class="card card-link" href="${ctx}/plan?examId=${exam.id}"><h3>6 · Study plan</h3><p class="muted small">AI-ordered, paced around your availability.</p></a>
            </div>
        </section>

        <section class="section">
            <div class="section__head"><h2>Edit details</h2></div>
            <div class="card">
                <form method="post" action="${ctx}/exams/update">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <input type="hidden" name="examId" value="${exam.id}">
                    <div class="field">
                        <label for="subjectName">Subject / exam name</label>
                        <input type="text" id="subjectName" name="subjectName" required maxlength="180" value="${fn:escapeXml(exam.subjectName)}">
                    </div>
                    <div class="form-grid">
                        <div class="field">
                            <label for="examDate">Exam date</label>
                            <input type="date" id="examDate" name="examDate" value="${exam.examDateInput}">
                        </div>
                        <div class="field">
                            <label for="examTime">Exam time</label>
                            <input type="time" id="examTime" name="examTime" value="${exam.examTimeInput}">
                        </div>
                        <div class="field">
                            <label for="timezone">Time zone</label>
                            <input type="text" id="timezone" name="timezone" value="${fn:escapeXml(exam.timezone)}" maxlength="64">
                        </div>
                        <div class="field">
                            <label for="totalMarks">Total marks</label>
                            <input type="number" id="totalMarks" name="totalMarks" min="1" value="${exam.totalMarks}">
                        </div>
                        <div class="field">
                            <label for="targetMarks">Target marks</label>
                            <input type="number" id="targetMarks" name="targetMarks" min="1" value="${exam.targetMarks}">
                        </div>
                        <div class="field">
                            <label for="status">Status</label>
                            <select id="status" name="status">
                                <option value="ACTIVE" ${exam.status eq 'ACTIVE' ? 'selected' : ''}>Active</option>
                                <option value="COMPLETED" ${exam.status eq 'COMPLETED' ? 'selected' : ''}>Completed</option>
                                <option value="ARCHIVED" ${exam.status eq 'ARCHIVED' ? 'selected' : ''}>Archived</option>
                            </select>
                        </div>
                    </div>
                    <div class="field">
                        <label for="notes">Notes</label>
                        <textarea id="notes" name="notes" maxlength="4000">${fn:escapeXml(exam.notes)}</textarea>
                    </div>
                    <div class="form-actions">
                        <button type="submit" class="btn btn--primary" data-working-label="Saving…">Save changes</button>
                    </div>
                </form>
            </div>
        </section>
    </div>

    <div class="stack">
        <section class="section">
            <div class="section__head"><h2>Weekly availability</h2><a class="hint" href="${ctx}/exams/availability?examId=${exam.id}">Edit</a></div>
            <div class="card">
                <c:choose>
                    <c:when test="${empty availability}">
                        <p class="muted small">No weekly availability set. The AI assumes a default daily budget.</p>
                    </c:when>
                    <c:otherwise>
                        <dl class="kv">
                            <c:forEach var="slot" items="${availability}">
                                <dt>${slot.dayLabel}</dt><dd>${slot.hoursDisplay}</dd>
                            </c:forEach>
                        </dl>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>

        <section class="section">
            <div class="section__head"><h2>Delete exam</h2></div>
            <div class="card">
                <p class="muted small">Deleting removes this exam and every document, plan, paper and assessment tied to it. This cannot be undone.</p>
                <form method="post" action="${ctx}/exams/delete" data-confirm="Delete this exam and all of its records? This cannot be undone.">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <input type="hidden" name="examId" value="${exam.id}">
                    <button type="submit" class="btn btn--danger">Delete exam and all records</button>
                </form>
            </div>
        </section>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
