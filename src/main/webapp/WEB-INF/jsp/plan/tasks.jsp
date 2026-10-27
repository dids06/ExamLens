<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Plan tasks" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>All plan tasks</h1>
        <p class="lede">Every task the AI scheduled, in order. Update status as you go — the next replan will use it.</p>
    </div>
    <div class="page-head__actions">
        <a class="btn" href="${ctx}/plan?examId=${exam.id}">Plan overview</a>
    </div>
</div>

<c:choose>
    <c:when test="${empty planView or empty planView.tasks}">
        <div class="empty"><h3>No tasks</h3><p class="muted">Generate a study plan to see scheduled tasks here.</p>
            <a class="btn btn--primary" href="${ctx}/plan?examId=${exam.id}">Go to plan</a></div>
    </c:when>
    <c:otherwise>
        <div class="table-wrap">
            <table class="table">
                <thead><tr><th>#</th><th>Task</th><th>Type</th><th>Date</th><th class="num">Min</th><th>Priority</th><th>Status</th><th></th></tr></thead>
                <tbody>
                    <c:forEach var="task" items="${planView.tasks}">
                        <tr>
                            <td>${task.taskOrder}</td>
                            <td>
                                <strong>${fn:escapeXml(task.title)}</strong>
                                <c:if test="${not empty task.studyMethod}"><div class="small muted">${fn:escapeXml(task.studyMethod)}</div></c:if>
                            </td>
                            <td>${task.typeLabel}</td>
                            <td class="nowrap">${empty task.scheduledDate ? '—' : task.scheduledDate}</td>
                            <td class="num">${task.estimatedMinutes}</td>
                            <td>${empty task.priority ? '—' : fn:toLowerCase(task.priority)}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${task.completed}"><span class="badge badge--success">completed</span></c:when>
                                    <c:when test="${task.overdue}"><span class="badge badge--danger">overdue</span></c:when>
                                    <c:otherwise><span class="badge badge--muted">${fn:toLowerCase(task.status)}</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td class="nowrap">
                                <form method="post" action="${ctx}/plan/task/status" class="row" style="gap:.3rem;">
                                    <input type="hidden" name="_csrf" value="${csrfToken}">
                                    <input type="hidden" name="examId" value="${exam.id}">
                                    <input type="hidden" name="taskId" value="${task.id}">
                                    <select name="status" aria-label="Status">
                                        <option value="PENDING" ${task.status eq 'PENDING' ? 'selected' : ''}>Pending</option>
                                        <option value="IN_PROGRESS" ${task.status eq 'IN_PROGRESS' ? 'selected' : ''}>In progress</option>
                                        <option value="COMPLETED" ${task.status eq 'COMPLETED' ? 'selected' : ''}>Completed</option>
                                        <option value="SKIPPED" ${task.status eq 'SKIPPED' ? 'selected' : ''}>Skipped</option>
                                    </select>
                                    <button type="submit" class="btn btn--sm">Save</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
