<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="AI assistant" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>AI assistant</h1>
        <p class="lede">Ask a question or request an explanation. Requests are classified first, then answered against your confirmed topics and sources.</p>
    </div>
</div>

<div class="split">
    <div class="stack">
        <section class="section">
            <div class="section__head"><h2>Chat</h2>
                <form method="get" action="${ctx}/assistant" class="row" style="gap:.4rem;">
                    <select name="examId" aria-label="Exam context">
                        <option value="">No exam context</option>
                        <c:forEach var="e" items="${exams}">
                            <option value="${e.id}" ${e.id eq selectedExamId ? 'selected' : ''}>${fn:escapeXml(e.subjectName)}</option>
                        </c:forEach>
                    </select>
                    <button type="submit" class="btn btn--sm">Switch</button>
                </form>
            </div>

            <div class="card">
                <c:choose>
                    <c:when test="${empty messages}">
                        <p class="muted small">No messages in this conversation yet. Ask anything about your subject or plan.</p>
                    </c:when>
                    <c:otherwise>
                        <div class="stack" style="max-height:460px;overflow-y:auto;padding-right:.2rem;">
                            <c:forEach var="m" items="${messages}">
                                <div class="panel ${m.user ? '' : 'panel--ai'}" style="align-self:${m.user ? 'flex-start' : 'flex-end'};max-width:88%;">
                                    <div class="row small muted" style="justify-content:space-between;">
                                        <span>${m.user ? 'You' : 'ExamLens AI'}</span>
                                        <span>${m.createdAt}</span>
                                    </div>
                                    <div style="white-space:pre-wrap;">${fn:escapeXml(m.content)}</div>
                                </div>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>

                <form method="post" action="${ctx}/assistant/chat" style="margin-top:1rem;">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <c:if test="${not empty selectedExamId}"><input type="hidden" name="examId" value="${selectedExamId}"></c:if>
                    <div class="field">
                        <label for="message">Your question</label>
                        <textarea id="message" name="message" maxlength="4000" required placeholder="e.g. Explain photosynthesis light reactions and how they are examined."></textarea>
                    </div>
                    <div class="row">
                        <div class="field" style="margin:0;max-width:200px;">
                            <label for="depth">Depth</label>
                            <select id="depth" name="depth">
                                <option value="BRIEF">Brief</option>
                                <option value="STANDARD" selected>Standard</option>
                                <option value="DEEP">Deep</option>
                            </select>
                        </div>
                        <div class="spacer"></div>
                        <button type="submit" class="btn btn--primary" data-working-label="Asking…">Send</button>
                    </div>
                </form>
            </div>
        </section>

        <section class="section">
            <div class="section__head"><h2>Explain a concept</h2></div>
            <div class="card">
                <form method="post" action="${ctx}/assistant/explain">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <c:if test="${not empty selectedExamId}"><input type="hidden" name="examId" value="${selectedExamId}"></c:if>
                    <div class="field">
                        <label for="question">Question or concept</label>
                        <textarea id="question" name="question" maxlength="4000" required placeholder="What should I understand about this?"></textarea>
                    </div>
                    <div class="form-grid">
                        <div class="field"><label for="topic">Topic (optional)</label><input type="text" id="topic" name="topic" maxlength="255"></div>
                        <div class="field"><label for="depth2">Depth</label>
                            <select id="depth2" name="depth"><option value="BRIEF">Brief</option><option value="STANDARD" selected>Standard</option><option value="DEEP">Deep</option></select>
                        </div>
                    </div>
                    <button type="submit" class="btn btn--primary" data-working-label="Generating…">Generate explanation</button>
                </form>
            </div>
        </section>
    </div>

    <div class="stack">
        <section class="section">
            <div class="section__head"><h2>Conversations</h2><a class="hint" href="${ctx}/assistant">New</a></div>
            <div class="card">
                <c:choose>
                    <c:when test="${empty conversations}"><p class="muted small">No conversations yet.</p></c:when>
                    <c:otherwise>
                        <ul class="list">
                            <c:forEach var="conv" items="${conversations}">
                                <li class="list__item">
                                    <div class="row row--between">
                                        <a href="${ctx}/assistant?conversationId=${conv.id}<c:if test="${not empty selectedExamId}">&amp;examId=${selectedExamId}</c:if>">${fn:escapeXml(conv.title)}</a>
                                        <form method="post" action="${ctx}/assistant/delete" data-confirm="Delete this conversation?">
                                            <input type="hidden" name="_csrf" value="${csrfToken}">
                                            <input type="hidden" name="conversationId" value="${conv.id}">
                                            <c:if test="${not empty selectedExamId}"><input type="hidden" name="examId" value="${selectedExamId}"></c:if>
                                            <button type="submit" class="btn btn--ghost btn--sm" aria-label="Delete conversation">&times;</button>
                                        </form>
                                    </div>
                                    <div class="small muted">${conv.updatedAt}</div>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>

        <section class="section">
            <div class="section__head"><h2>Explanation history</h2></div>
            <c:choose>
                <c:when test="${empty explanations}">
                    <div class="card"><p class="muted small">No saved explanations yet.</p></div>
                </c:when>
                <c:otherwise>
                    <div class="stack">
                        <c:forEach var="x" items="${explanations}">
                            <details class="disclosure">
                                <summary>${fn:escapeXml(x.question)}</summary>
                                <div class="disclosure__body">
                                    <c:if test="${not empty x.topicTitle}"><span class="badge badge--ai">${fn:escapeXml(x.topicTitle)}</span></c:if>
                                    <c:if test="${x.simulated}"><span class="badge badge--warn">simulated</span></c:if>
                                    <p style="margin-top:.5rem;"><strong>${fn:escapeXml(x.directAnswer)}</strong></p>
                                    <div class="prose"><p>${fn:escapeXml(x.explanation)}</p></div>
                                    <c:if test="${not empty x.exampleList}">
                                        <h4>Examples</h4>
                                        <ul class="clean small"><c:forEach var="ex" items="${x.exampleList}"><li>${fn:escapeXml(ex)}</li></c:forEach></ul>
                                    </c:if>
                                    <c:if test="${not empty x.limitationList}">
                                        <h4>Limitations</h4>
                                        <ul class="clean cross small"><c:forEach var="l" items="${x.limitationList}"><li>${fn:escapeXml(l)}</li></c:forEach></ul>
                                    </c:if>
                                </div>
                            </details>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
