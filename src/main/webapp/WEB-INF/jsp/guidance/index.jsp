<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Teacher guidance" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>Teacher guidance</h1>
        <p class="lede">Turn teacher notes and instructions into hints and priorities. Everything stays a draft until you confirm it.</p>
    </div>
</div>

<div class="split">
    <div class="stack">
        <c:choose>
            <c:when test="${empty guidance}">
                <div class="empty">
                    <h3>No guidance extracted yet</h3>
                    <p class="muted">Upload your teacher's notes, then extract the hints and important topics.</p>
                </div>
            </c:when>
            <c:otherwise>
                <section class="section">
                    <div class="section__head">
                        <h2>Extracted guidance</h2>
                        <c:choose>
                            <c:when test="${guidance.confirmed}"><span class="badge badge--success">confirmed</span></c:when>
                            <c:otherwise><span class="badge badge--warn">draft · not confirmed</span></c:otherwise>
                        </c:choose>
                    </div>
                    <div class="card">
                        <div class="row row--between" style="margin-bottom:.5rem;">
                            <span class="badge badge--ai">AI extraction</span>
                            <c:if test="${guidance.simulated}"><span class="badge badge--warn">simulated</span></c:if>
                        </div>

                        <c:if test="${not empty guidance.hintList}">
                            <h3>Hints</h3>
                            <ul class="list">
                                <c:forEach var="h" items="${guidance.hintList}">
                                    <li class="list__item">
                                        <div>${fn:escapeXml(h.text)}</div>
                                        <c:if test="${not empty h.evidenceQuote}"><div class="small muted">“${fn:escapeXml(h.evidenceQuote)}”</div></c:if>
                                        <div class="row small muted">
                                            <c:if test="${not empty h.sourceReference}"><span>${fn:escapeXml(h.sourceReference)}</span></c:if>
                                            <c:if test="${not empty h.confidence}"><span>· ${fn:toLowerCase(h.confidence)} confidence</span></c:if>
                                        </div>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:if>

                        <c:if test="${not empty guidance.importantTopicList}">
                            <h3 style="margin-top:1rem;">Important topics</h3>
                            <div class="table-wrap">
                                <table class="table">
                                    <thead><tr><th>Topic</th><th>Priority</th><th>Evidence</th></tr></thead>
                                    <tbody>
                                        <c:forEach var="t" items="${guidance.importantTopicList}">
                                            <tr>
                                                <td>${fn:escapeXml(t.topic)}</td>
                                                <td>${empty t.priority ? '—' : fn:toLowerCase(t.priority)}</td>
                                                <td class="small muted">${fn:escapeXml(t.evidenceQuote)}</td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:if>

                        <c:if test="${not empty guidance.instructionLines}">
                            <h3 style="margin-top:1rem;">Instructions</h3>
                            <ul class="clean tick small">
                                <c:forEach var="i" items="${guidance.instructionLines}"><li>${fn:escapeXml(i)}</li></c:forEach>
                            </ul>
                        </c:if>

                        <c:if test="${not empty guidance.formatLines}">
                            <h3 style="margin-top:1rem;">Exam format</h3>
                            <ul class="clean tick small">
                                <c:forEach var="f" items="${guidance.formatLines}"><li>${fn:escapeXml(f)}</li></c:forEach>
                            </ul>
                        </c:if>

                        <c:if test="${not empty guidance.uncertaintyLines}">
                            <h3 style="margin-top:1rem;">Uncertainties</h3>
                            <ul class="clean cross small">
                                <c:forEach var="u" items="${guidance.uncertaintyLines}"><li>${fn:escapeXml(u)}</li></c:forEach>
                            </ul>
                        </c:if>

                        <c:if test="${not guidance.confirmed}">
                            <div class="form-actions" style="margin-top:1rem;">
                                <form method="post" action="${ctx}/guidance/confirm">
                                    <input type="hidden" name="_csrf" value="${csrfToken}">
                                    <input type="hidden" name="examId" value="${exam.id}">
                                    <input type="hidden" name="guidanceId" value="${guidance.id}">
                                    <button type="submit" class="btn btn--primary">Confirm guidance</button>
                                </form>
                            </div>
                        </c:if>
                    </div>
                </section>
            </c:otherwise>
        </c:choose>
    </div>

    <section class="section">
        <div class="section__head"><h2>Extract from a document</h2></div>
        <div class="card">
            <c:choose>
                <c:when test="${empty documents}">
                    <p class="muted small">No documents uploaded yet.</p>
                    <a class="btn btn--sm" href="${ctx}/documents?examId=${exam.id}">Upload a document</a>
                </c:when>
                <c:otherwise>
                    <form method="post" action="${ctx}/guidance/extract">
                        <input type="hidden" name="_csrf" value="${csrfToken}">
                        <input type="hidden" name="examId" value="${exam.id}">
                        <div class="field">
                            <label for="documentId">Teacher notes document</label>
                            <select id="documentId" name="documentId">
                                <c:forEach var="doc" items="${documents}">
                                    <option value="${doc.id}" ${doc.documentType eq 'TEACHER_NOTES' ? 'selected' : ''}>${fn:escapeXml(doc.originalFilename)} · ${doc.typeLabel}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <button type="submit" class="btn btn--primary btn--block" data-working-label="Extracting…">Extract guidance</button>
                    </form>
                    <p class="help" style="margin-top:.5rem;">Hints cite the exact source lines they came from.</p>
                </c:otherwise>
            </c:choose>
        </div>
    </section>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
