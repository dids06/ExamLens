<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Revision" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>Revision resources</h1>
        <p class="lede">The AI chooses which topics deserve key points, flashcards and active-recall questions — and how deep to go.</p>
    </div>
    <div class="page-head__actions"><span class="pill">${resourceCount} set<c:if test="${resourceCount ne 1}">s</c:if></span></div>
</div>

<div class="split">
    <section class="section">
        <div class="section__head"><h2>Your revision sets</h2></div>
        <c:choose>
            <c:when test="${empty resources}">
                <div class="empty"><h3>No revision resources yet</h3><p class="muted">Generate a set from your confirmed topics to build flashcards and recall questions.</p></div>
            </c:when>
            <c:otherwise>
                <div class="stack">
                    <c:forEach var="r" items="${resources}">
                        <div class="card">
                            <div class="row row--between">
                                <h3>${fn:escapeXml(r.topicTitle)}</h3>
                                <span class="row">
                                    <span class="badge badge--ai">AI revision</span>
                                    <c:if test="${r.simulated}"><span class="badge badge--warn">simulated</span></c:if>
                                </span>
                            </div>
                            <c:if test="${not empty r.keyPointList}">
                                <h4 style="margin-top:.6rem;">Key points</h4>
                                <ul class="clean tick small"><c:forEach var="kp" items="${r.keyPointList}"><li>${fn:escapeXml(kp)}</li></c:forEach></ul>
                            </c:if>
                            <c:if test="${not empty r.flashcardList}">
                                <h4 style="margin-top:.6rem;">Flashcards</h4>
                                <div class="grid grid--2">
                                    <c:forEach var="card" items="${r.flashcardList}" varStatus="s">
                                        <div class="flip">
                                            <div class="flip__front">${fn:escapeXml(card.front)}</div>
                                            <div class="flip__back hidden" id="fc-${r.id}-${s.index}">${fn:escapeXml(card.back)}</div>
                                            <button type="button" class="btn btn--ghost btn--sm" data-reveal="fc-${r.id}-${s.index}">Show answer</button>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:if>
                            <c:if test="${not empty r.recallList}">
                                <h4 style="margin-top:.6rem;">Active recall</h4>
                                <ul class="list">
                                    <c:forEach var="rc" items="${r.recallList}">
                                        <li class="list__item">
                                            <div><strong>${fn:escapeXml(rc.question)}</strong></div>
                                            <div class="small muted">${fn:escapeXml(rc.answer)}</div>
                                        </li>
                                    </c:forEach>
                                </ul>
                            </c:if>
                            <c:if test="${not empty r.sourceReferences}">
                                <p class="small muted" style="margin-top:.5rem;">Sources: ${fn:escapeXml(r.sourceReferences)}</p>
                            </c:if>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </section>

    <section class="section">
        <div class="section__head"><h2>Generate revision</h2></div>
        <div class="card">
            <c:choose>
                <c:when test="${empty topics}">
                    <p class="muted small">Confirm your syllabus topics first.</p>
                    <a class="btn btn--sm" href="${ctx}/syllabus?examId=${exam.id}">Go to syllabus</a>
                </c:when>
                <c:otherwise>
                    <form method="post" action="${ctx}/revision/generate">
                        <input type="hidden" name="_csrf" value="${csrfToken}">
                        <input type="hidden" name="examId" value="${exam.id}">
                        <fieldset>
                            <legend>Topics <span class="small muted">(all confirmed if none selected)</span></legend>
                            <label class="check" style="margin-bottom:.4rem;"><input type="checkbox" data-check-all data-check-target="topicIds"> Select all</label>
                            <div class="stack" style="max-height:240px;overflow-y:auto;">
                                <c:forEach var="topic" items="${topics}">
                                    <label class="check"><input type="checkbox" name="topicIds" value="${topic.id}"> ${fn:escapeXml(topic.title)}</label>
                                </c:forEach>
                            </div>
                        </fieldset>
                        <div class="form-grid">
                            <div class="field"><label for="depth">Depth</label>
                                <select id="depth" name="depth"><option value="QUICK">Quick</option><option value="STANDARD" selected>Standard</option><option value="DEEP">Deep</option></select>
                            </div>
                            <div class="field"><label for="availableMinutes">Available minutes</label><input type="number" id="availableMinutes" name="availableMinutes" min="5" max="1440" placeholder="optional"></div>
                        </div>
                        <button type="submit" class="btn btn--primary btn--block" data-working-label="Generating…">Generate revision set</button>
                    </form>
                </c:otherwise>
            </c:choose>
        </div>
    </section>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
