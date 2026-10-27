<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Practice attempt" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>${fn:escapeXml(paper.title)}</h1>
        <p class="lede">
            Attempt started ${view.attempt.startedAt}
            <c:if test="${view.attempt.timed and not empty view.attempt.durationMinutes}"> · timed, ${view.attempt.durationMinutes} min</c:if>
        </p>
    </div>
    <div class="page-head__actions">
        <c:choose>
            <c:when test="${view.attempt.submitted}"><span class="badge badge--success">submitted</span></c:when>
            <c:otherwise><span class="badge badge--warn">in progress</span></c:otherwise>
        </c:choose>
        <a class="btn" href="${ctx}/practice/paper?id=${paper.id}">Back to paper</a>
    </div>
</div>

<c:choose>
    <c:when test="${view.attempt.submitted}">
        <div class="grid grid--3 section">
            <div class="card stat"><div class="stat__label">Score</div><div class="stat__value">${view.attempt.scoreDisplay}</div><div class="stat__sub">awarded / possible</div></div>
            <div class="card stat"><div class="stat__label">Percentage</div><div class="stat__value">${view.attempt.scorePercent}%</div><div class="stat__sub">overall</div></div>
            <div class="card stat"><div class="stat__label">Submitted</div><div class="stat__value" style="font-size:1.05rem;">${view.attempt.submittedAt}</div><div class="stat__sub">graded</div></div>
        </div>

        <section class="section">
            <div class="section__head"><h2>Reviewed answers</h2></div>
            <div class="card">
                <c:forEach var="a" items="${view.answers}" varStatus="s">
                    <div class="question">
                        <div class="question__head">
                            <span class="badge badge--muted">Q${s.index + 1}</span>
                            <c:if test="${not empty a.question}">
                                <span class="badge badge--user">${a.question.typeLabel}</span>
                                <span class="badge badge--muted">${a.question.marks} mark<c:if test="${a.question.marks ne 1}">s</c:if></span>
                            </c:if>
                            <span class="badge ${a.aiGraded ? 'badge--ai' : 'badge--user'}">${a.aiGraded ? 'AI graded' : 'auto graded'}</span>
                            <span class="badge badge--primary">${a.awardedDisplay}</span>
                            <c:if test="${a.requiresManualReview}"><span class="badge badge--warn">review suggested</span></c:if>
                        </div>
                        <c:if test="${not empty a.question}">
                            <div class="question__prompt">${fn:escapeXml(a.question.prompt)}</div>
                            <c:if test="${not empty a.question.options}">
                                <ul class="options">
                                    <c:forEach var="opt" items="${a.question.options}" varStatus="o">
                                        <c:set var="label" value="${a.question.optionLabels[o.index]}" />
                                        <li class="option" style="${label eq a.question.correctOption ? 'border-color:var(--success);' : ''}">
                                            <span class="option__label">${label}</span><span>${fn:escapeXml(opt)}</span>
                                            <c:if test="${label eq a.question.correctOption}"><span class="badge badge--success">correct</span></c:if>
                                            <c:if test="${label eq a.selectedOption and label ne a.question.correctOption}"><span class="badge badge--danger">your answer</span></c:if>
                                        </li>
                                    </c:forEach>
                                </ul>
                            </c:if>
                        </c:if>
                        <c:if test="${not empty a.answerText}">
                            <div class="answer-feedback"><strong>Your answer:</strong> <span style="white-space:pre-wrap;">${fn:escapeXml(a.answerText)}</span></div>
                        </c:if>
                        <c:if test="${not empty a.question.modelAnswer}">
                            <div class="answer-feedback"><strong>Model answer:</strong> <span style="white-space:pre-wrap;">${fn:escapeXml(a.question.modelAnswer)}</span></div>
                        </c:if>
                        <c:if test="${not empty a.feedback}">
                            <div class="answer-feedback"><strong>Feedback:</strong> ${fn:escapeXml(a.feedback)}</div>
                        </c:if>
                        <c:if test="${not empty a.criteria}">
                            <ul class="clean small" style="margin-top:.4rem;">
                                <c:forEach var="crit" items="${a.criteria}">
                                    <li>${fn:escapeXml(crit.criterion)} · ${crit.awardedMarks}/${crit.maximumMarks}
                                        <c:if test="${not empty crit.reason}"><span class="muted">— ${fn:escapeXml(crit.reason)}</span></c:if></li>
                                </c:forEach>
                            </ul>
                        </c:if>
                    </div>
                </c:forEach>
            </div>
        </section>
    </c:when>
    <c:otherwise>
        <div class="ai-note section">Answer what you can. Multiple-choice questions are graded instantly; written answers are graded by the AI against the rubric.</div>
        <form method="post" action="${ctx}/practice/submit">
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <input type="hidden" name="examId" value="${exam.id}">
            <input type="hidden" name="attemptId" value="${view.attempt.id}">
            <div class="card">
                <c:forEach var="q" items="${view.questions}" varStatus="s">
                    <div class="question">
                        <div class="question__head">
                            <span class="badge badge--muted">Q${s.index + 1}</span>
                            <span class="badge badge--user">${q.typeLabel}</span>
                            <span class="badge badge--muted">${q.marks} mark<c:if test="${q.marks ne 1}">s</c:if></span>
                            <c:if test="${not empty q.topic}"><span class="small muted">${fn:escapeXml(q.topic)}</span></c:if>
                        </div>
                        <div class="question__prompt">${fn:escapeXml(q.prompt)}</div>
                        <c:choose>
                            <c:when test="${q.multipleChoice}">
                                <ul class="options">
                                    <c:forEach var="opt" items="${q.options}" varStatus="o">
                                        <label class="option">
                                            <input type="radio" name="q_${q.id}" value="${q.optionLabels[o.index]}">
                                            <span class="option__label">${q.optionLabels[o.index]}</span>
                                            <span>${fn:escapeXml(opt)}</span>
                                        </label>
                                    </c:forEach>
                                </ul>
                            </c:when>
                            <c:otherwise>
                                <div class="field" style="margin-top:.5rem;">
                                    <label for="q_${q.id}">Your answer</label>
                                    <textarea id="q_${q.id}" name="q_${q.id}" maxlength="8000" placeholder="Write your answer…"></textarea>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </c:forEach>
                <div class="form-actions">
                    <button type="submit" class="btn btn--primary" data-working-label="Submitting…">Submit attempt</button>
                </div>
            </div>
        </form>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
