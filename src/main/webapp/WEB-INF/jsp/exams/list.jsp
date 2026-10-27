<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Exams" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>Exams</h1>
        <p class="lede">Each exam keeps its own sources, syllabus, plan and practice history, completely separate from the others.</p>
    </div>
    <div class="page-head__actions">
        <a class="btn btn--primary" href="${ctx}/exams/new">New exam</a>
    </div>
</div>

<c:choose>
    <c:when test="${empty exams}">
        <div class="empty">
            <h3>No exams yet</h3>
            <p class="muted">Create the exam you are preparing for to get started.</p>
            <a class="btn btn--primary" href="${ctx}/exams/new">Create an exam</a>
        </div>
    </c:when>
    <c:otherwise>
        <div class="grid grid--2">
            <c:forEach var="exam" items="${exams}">
                <a class="card card-link" href="${ctx}/exams/view?id=${exam.id}">
                    <div class="row row--between">
                        <h3>${fn:escapeXml(exam.subjectName)}</h3>
                        <c:choose>
                            <c:when test="${exam.id eq activeExamId}"><span class="badge badge--primary">active</span></c:when>
                            <c:otherwise><span class="badge badge--muted">${fn:toLowerCase(exam.status)}</span></c:otherwise>
                        </c:choose>
                    </div>
                    <p class="muted small" style="margin:.3rem 0 .6rem;">
                        <c:choose>
                            <c:when test="${not empty exam.examDatetime}">
                                ${exam.examDatetimeDisplay}
                                <c:if test="${not empty exam.daysUntil}"> · ${exam.daysUntil} day<c:if test="${exam.daysUntil ne 1}">s</c:if> to go</c:if>
                            </c:when>
                            <c:otherwise>No exam date set</c:otherwise>
                        </c:choose>
                    </p>
                    <div class="row small muted">
                        <c:if test="${not empty exam.totalMarks}"><span>Total marks ${exam.totalMarks}</span></c:if>
                        <c:if test="${not empty exam.targetMarks}"><span>· Target ${exam.targetMarks}</span></c:if>
                        <c:if test="${empty exam.totalMarks and empty exam.targetMarks}"><span>No marks set</span></c:if>
                    </div>
                </a>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
