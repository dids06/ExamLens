<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="New exam" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>Create an exam</h1>
        <p class="lede">Give ExamLens the essentials. You can refine everything later, and the AI will use these details to plan.</p>
    </div>
</div>

<div class="card" style="max-width:640px;">
    <form method="post" action="${ctx}/exams/create">
        <input type="hidden" name="_csrf" value="${csrfToken}">
        <div class="field">
            <label for="subjectName">Subject / exam name</label>
            <input type="text" id="subjectName" name="subjectName" required maxlength="180" placeholder="e.g. A-Level Biology">
        </div>
        <div class="form-grid">
            <div class="field">
                <label for="examDate">Exam date</label>
                <input type="date" id="examDate" name="examDate">
            </div>
            <div class="field">
                <label for="examTime">Exam time</label>
                <input type="time" id="examTime" name="examTime">
            </div>
            <div class="field">
                <label for="timezone">Time zone</label>
                <input type="text" id="timezone" name="timezone" value="UTC" maxlength="64">
            </div>
            <div class="field">
                <label for="totalMarks">Total marks</label>
                <input type="number" id="totalMarks" name="totalMarks" min="1" max="10000" placeholder="optional">
            </div>
            <div class="field">
                <label for="targetMarks">Target marks</label>
                <input type="number" id="targetMarks" name="targetMarks" min="1" max="10000" placeholder="optional">
            </div>
        </div>
        <div class="field">
            <label for="notes">Notes</label>
            <textarea id="notes" name="notes" maxlength="4000" placeholder="Anything the AI should know about this exam (format, weighting, constraints)."></textarea>
            <span class="help" id="notesCount" data-counter-for="notes"></span>
        </div>
        <div class="form-actions">
            <button type="submit" class="btn btn--primary" data-working-label="Creating…">Create exam</button>
            <a class="btn" href="${ctx}/exams">Cancel</a>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
