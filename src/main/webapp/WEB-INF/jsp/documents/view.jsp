<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Document" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>${fn:escapeXml(document.originalFilename)}</h1>
        <p class="lede">${document.typeLabel} · uploaded ${document.createdAt}</p>
    </div>
    <div class="page-head__actions">
        <form method="post" action="${ctx}/documents/delete" data-confirm="Delete this document?">
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <input type="hidden" name="documentId" value="${document.id}">
            <button type="submit" class="btn btn--danger">Delete</button>
        </form>
        <a class="btn" href="${ctx}/documents?examId=${document.examId}">Back to documents</a>
    </div>
</div>

<div class="grid grid--4 section">
    <div class="card stat"><div class="stat__label">Size</div><div class="stat__value" style="font-size:1.2rem;">${document.sizeDisplay}</div><div class="stat__sub">${document.contentType}</div></div>
    <div class="card stat"><div class="stat__label">Pages</div><div class="stat__value" style="font-size:1.2rem;"><c:choose><c:when test="${not empty document.pageCount}">${document.pageCount}</c:when><c:otherwise>—</c:otherwise></c:choose></div><div class="stat__sub">Detected</div></div>
    <div class="card stat"><div class="stat__label">Extracted</div><div class="stat__value" style="font-size:1.2rem;">${document.extractedChars}</div><div class="stat__sub">characters</div></div>
    <div class="card stat"><div class="stat__label">Method</div><div class="stat__value" style="font-size:1.05rem;word-break:break-word;">${empty document.extractionMethod ? '—' : document.extractionMethod}</div><div class="stat__sub">${document.extractionStatus}</div></div>
</div>

<c:if test="${not empty document.extractionWarnings}">
    <div class="flash flash--info" style="margin-bottom:1rem;">
        <span aria-hidden="true">i</span>
        <div class="flash__body">${fn:escapeXml(document.extractionWarnings)}</div>
    </div>
</c:if>

<section class="section">
    <div class="section__head"><h2>Extracted text</h2><span class="hint">${fn:length(blocks)} block<c:if test="${fn:length(blocks) ne 1}">s</c:if></span></div>
    <c:choose>
        <c:when test="${empty blocks}">
            <div class="empty">
                <h3>No text stored</h3>
                <p class="muted">No readable text was found in this file. If it is a scan, try a text-based export or a multimodal model.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="stack">
                <c:forEach var="block" items="${blocks}">
                    <div class="card">
                        <div class="row row--between" style="margin-bottom:.3rem;">
                            <strong>${empty block.heading ? 'Section' : fn:escapeXml(block.heading)}</strong>
                            <span class="row small muted">
                                <span>${block.pageLabel}</span>
                                <c:if test="${block.uncertain}"><span class="badge badge--warn">uncertain</span></c:if>
                            </span>
                        </div>
                        <p style="white-space:pre-wrap;margin:0;">${fn:escapeXml(block.blockText)}</p>
                    </div>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
