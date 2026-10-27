<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Study availability" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>Weekly availability</h1>
        <p class="lede">Tell ExamLens how many minutes you can realistically study each day. The AI uses this to pace your plan and to judge feasibility.</p>
    </div>
</div>

<div class="card" style="max-width:640px;">
    <form method="post" action="${ctx}/exams/availability">
        <input type="hidden" name="_csrf" value="${csrfToken}">
        <input type="hidden" name="examId" value="${exam.id}">
        <div class="form-grid">
            <c:forEach var="d" items="${days}">
                <div class="field">
                    <label for="day-${d.day}">${d.label}</label>
                    <input type="number" id="day-${d.day}" name="${d.field}" min="0" max="1440" step="15" value="${d.minutes}">
                </div>
            </c:forEach>
        </div>
        <p class="help">Total declared: ${weeklyMinutes} minutes per week. Leave a day at 0 if you cannot study then.</p>
        <div class="form-actions">
            <button type="submit" class="btn btn--primary" data-working-label="Saving…">Save availability</button>
            <a class="btn" href="${ctx}/exams/view?id=${exam.id}">Back to exam</a>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
