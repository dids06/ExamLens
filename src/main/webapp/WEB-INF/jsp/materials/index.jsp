<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Materials" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>Materials</h1>
        <p class="lede">Index each study source and compare them. Coverage judgements cite only the sources you supplied.</p>
    </div>
    <div class="page-head__actions">
        <form method="post" action="${ctx}/materials/compare">
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <input type="hidden" name="examId" value="${exam.id}">
            <button type="submit" class="btn btn--primary" data-working-label="Comparing…">Compare sources</button>
        </form>
    </div>
</div>

<div class="split">
    <div class="stack">
        <section class="section">
            <div class="section__head"><h2>Indexed sources</h2><span class="hint">${fn:length(indexes)} index<c:if test="${fn:length(indexes) ne 1}">es</c:if></span></div>
            <c:choose>
                <c:when test="${empty indexes}">
                    <div class="empty"><h3>No material indexed</h3><p class="muted">Index a study-material document to see section coverage against your topics.</p></div>
                </c:when>
                <c:otherwise>
                    <div class="stack">
                        <c:forEach var="index" items="${indexes}">
                            <div class="card">
                                <div class="row row--between">
                                    <h3>${fn:escapeXml(index.documentName)}</h3>
                                    <span class="row">
                                        <span class="badge badge--ai">AI index</span>
                                        <c:if test="${index.simulated}"><span class="badge badge--warn">simulated</span></c:if>
                                    </span>
                                </div>
                                <c:forEach var="section" items="${index.sectionList}">
                                    <div style="margin-top:.7rem;">
                                        <strong>${fn:escapeXml(section.title)}</strong>
                                        <span class="small muted">
                                            <c:if test="${not empty section.startPage}"> · pages ${section.startPage}<c:if test="${not empty section.endPage and section.endPage ne section.startPage}">–${section.endPage}</c:if></c:if>
                                        </span>
                                        <c:if test="${not empty section.topics}">
                                            <ul class="clean small" style="margin-top:.3rem;">
                                                <c:forEach var="t" items="${section.topics}">
                                                    <li>${fn:escapeXml(t.topic)} <span class="badge badge--muted">${fn:toLowerCase(t.coverage)}</span>
                                                        <c:if test="${not empty t.evidence}"><span class="muted">— ${fn:escapeXml(t.evidence)}</span></c:if>
                                                    </li>
                                                </c:forEach>
                                            </ul>
                                        </c:if>
                                    </div>
                                </c:forEach>
                                <c:if test="${not empty index.unmappedList}">
                                    <p class="small muted" style="margin-top:.6rem;">Not mapped to a confirmed topic:
                                        <c:forEach var="u" items="${index.unmappedList}" varStatus="us">${fn:escapeXml(u)}<c:if test="${!us.last}">, </c:if></c:forEach>
                                    </p>
                                </c:if>
                                <c:if test="${not empty index.warningList}">
                                    <div class="flash flash--info small"><div class="flash__body"><c:forEach var="w" items="${index.warningList}" varStatus="ws">${fn:escapeXml(w)}<c:if test="${!ws.last}"> </c:if></c:forEach></div></div>
                                </c:if>
                            </div>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </div>

    <div class="stack">
        <section class="section">
            <div class="section__head"><h2>Index a document</h2></div>
            <div class="card">
                <c:choose>
                    <c:when test="${empty documents}">
                        <p class="muted small">No documents uploaded yet.</p>
                        <a class="btn btn--sm" href="${ctx}/documents?examId=${exam.id}">Upload a document</a>
                    </c:when>
                    <c:otherwise>
                        <form method="post" action="${ctx}/materials/index">
                            <input type="hidden" name="_csrf" value="${csrfToken}">
                            <input type="hidden" name="examId" value="${exam.id}">
                            <div class="field">
                                <label for="documentId">Document</label>
                                <select id="documentId" name="documentId">
                                    <c:forEach var="doc" items="${documents}">
                                        <option value="${doc.id}">${fn:escapeXml(doc.originalFilename)} · ${doc.typeLabel}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <button type="submit" class="btn btn--primary btn--block" data-working-label="Indexing…">Index material</button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>

        <c:if test="${not empty comparison}">
            <section class="section">
                <div class="section__head"><h2>Source comparison</h2></div>
                <div class="card">
                    <c:if test="${comparison.simulated}"><span class="badge badge--warn">simulated</span></c:if>
                    <c:forEach var="rec" items="${comparison.recommendationList}">
                        <div class="list__item">
                            <div class="row row--between"><strong>${fn:escapeXml(rec.topic)}</strong>
                                <c:if test="${not empty rec.confidence}"><span class="badge badge--muted">${fn:toLowerCase(rec.confidence)}</span></c:if>
                            </div>
                            <div class="small">Best source: ${fn:escapeXml(rec.primarySourceName)}</div>
                            <c:if test="${not empty rec.rationale}"><div class="small muted">${fn:escapeXml(rec.rationale)}</div></c:if>
                        </div>
                    </c:forEach>
                    <c:if test="${not empty comparison.uncoveredList}">
                        <h3 style="margin-top:.8rem;">Uncovered topics</h3>
                        <ul class="clean cross small">
                            <c:forEach var="u" items="${comparison.uncoveredList}"><li>${fn:escapeXml(u)}</li></c:forEach>
                        </ul>
                    </c:if>
                    <c:if test="${not empty comparison.warningList}">
                        <div class="flash flash--info small"><div class="flash__body"><c:forEach var="w" items="${comparison.warningList}" varStatus="ws">${fn:escapeXml(w)}<c:if test="${!ws.last}"> </c:if></c:forEach></div></div>
                    </c:if>
                </div>
            </section>
        </c:if>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
