<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Practice paper" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>${fn:escapeXml(paper.title)}</h1>
        <p class="lede">${paper.totalMarks} marks · ${paper.durationDisplay}<c:if test="${not empty paper.difficulty}"> · ${fn:toLowerCase(paper.difficulty)}</c:if></p>
    </div>
    <div class="page-head__actions">
        <form method="post" action="${ctx}/practice/solutions">
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <input type="hidden" name="examId" value="${exam.id}">
            <input type="hidden" name="paperId" value="${paper.id}">
            <button type="submit" class="btn" data-working-label="Preparing…">Prepare answer key</button>
        </form>
        <a class="btn" href="${ctx}/practice?examId=${exam.id}">Back</a>
    </div>
</div>

<c:if test="${not empty paper.instructions}">
    <div class="card section"><strong>Instructions</strong><p style="margin:.3rem 0 0;">${fn:escapeXml(paper.instructions)}</p></div>
</c:if>

<div class="split">
    <section class="section">
        <div class="section__head"><h2>Questions</h2><span class="hint">${fn:length(paper.questions)} question<c:if test="${fn:length(paper.questions) ne 1}">s</c:if></span></div>
        <div class="card">
            <c:forEach var="q" items="${paper.questions}" varStatus="s">
                <div class="question">
                    <div class="question__head">
                        <span class="badge badge--muted">Q${s.index + 1}</span>
                        <span class="badge badge--user">${q.typeLabel}</span>
                        <span class="badge badge--muted">${q.marks} mark<c:if test="${q.marks ne 1}">s</c:if></span>
                        <c:if test="${not empty q.difficulty}"><span class="badge badge--muted">${fn:toLowerCase(q.difficulty)}</span></c:if>
                        <c:if test="${not empty q.topic}"><span class="small muted">${fn:escapeXml(q.topic)}</span></c:if>
                    </div>
                    <div class="question__prompt">${fn:escapeXml(q.prompt)}</div>
                    <c:if test="${not empty q.options}">
                        <ul class="options">
                            <c:forEach var="opt" items="${q.options}" varStatus="o">
                                <li class="option"><span class="option__label">${q.optionLabels[o.index]}</span><span>${fn:escapeXml(opt)}</span></li>
                            </c:forEach>
                        </ul>
                    </c:if>
                    <c:if test="${not empty q.modelAnswer or not empty q.rubric}">
                        <details class="disclosure" style="margin-top:.5rem;">
                            <summary>Answer &amp; rubric</summary>
                            <div class="disclosure__body">
                                <c:if test="${q.multipleChoice and not empty q.correctOption}">
                                    <p><strong>Correct option:</strong> ${fn:escapeXml(q.correctOption)}</p>
                                </c:if>
                                <c:if test="${not empty q.modelAnswer}">
                                    <p><strong>Model answer:</strong></p>
                                    <div class="prose"><p>${fn:escapeXml(q.modelAnswer)}</p></div>
                                </c:if>
                                <c:if test="${not empty q.rubric}">
                                    <p><strong>Rubric:</strong></p>
                                    <ul class="clean small">
                                        <c:forEach var="r" items="${q.rubric}"><li>${fn:escapeXml(r.criterion)} — ${r.marks} mark<c:if test="${r.marks ne 1}">s</c:if></li></c:forEach>
                                    </ul>
                                </c:if>
                            </div>
                        </details>
                    </c:if>
                </div>
            </c:forEach>
        </div>
    </section>

    <div class="stack">
        <section class="section">
            <div class="section__head"><h2>Start an attempt</h2></div>
            <div class="card">
                <form method="post" action="${ctx}/practice/start">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <input type="hidden" name="examId" value="${exam.id}">
                    <input type="hidden" name="paperId" value="${paper.id}">
                    <label class="check"><input type="checkbox" name="timed" value="true"> Timed attempt</label>
                    <div class="field" style="margin-top:.5rem;">
                        <label for="durationMinutes">Duration (minutes)</label>
                        <input type="number" id="durationMinutes" name="durationMinutes" min="1" max="600" value="${paper.durationMinutes}">
                        <span class="help">Used only when timed is ticked. Defaults to the paper's duration.</span>
                    </div>
                    <button type="submit" class="btn btn--primary btn--block" data-working-label="Starting…">Start attempt</button>
                </form>
                <p class="help" style="margin-top:.5rem;">Written answers are graded by the AI against the rubric; multiple-choice answers are graded on the server.</p>
            </div>
        </section>

        <section class="section">
            <div class="section__head"><h2>Attempts</h2></div>
            <c:choose>
                <c:when test="${empty attempts}"><div class="card"><p class="muted small">No attempts yet.</p></div></c:when>
                <c:otherwise>
                    <div class="card card--flush">
                        <table class="table">
                            <thead><tr><th>Started</th><th>Score</th><th></th></tr></thead>
                            <tbody>
                                <c:forEach var="a" items="${attempts}">
                                    <tr>
                                        <td>${a.startedAt}<div class="small muted">${a.status}</div></td>
                                        <td>${a.scoreDisplay}</td>
                                        <td class="num"><a class="btn btn--sm" href="${ctx}/practice/attempt?id=${a.id}">${a.submitted ? 'Results' : 'Resume'}</a></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
