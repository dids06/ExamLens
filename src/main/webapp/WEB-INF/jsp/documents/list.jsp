<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Documents" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>Documents</h1>
        <p class="lede">Upload the sources for ${fn:escapeXml(exam.subjectName)}. Text is extracted on upload; scanned files need OCR or a multimodal model.</p>
    </div>
</div>

<div class="split">
    <section class="section">
        <div class="section__head"><h2>Uploaded sources</h2><span class="hint">${fn:length(documents)} file<c:if test="${fn:length(documents) ne 1}">s</c:if></span></div>
        <c:choose>
            <c:when test="${empty documents}">
                <div class="empty">
                    <h3>No documents yet</h3>
                    <p class="muted">Upload your syllabus first — it unlocks topic extraction and planning.</p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table class="table">
                        <thead>
                            <tr><th>File</th><th>Type</th><th>Extraction</th><th class="num">Size</th><th></th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="doc" items="${documents}">
                                <tr>
                                    <td>
                                        <a href="${ctx}/documents/view?id=${doc.id}">${fn:escapeXml(doc.originalFilename)}</a>
                                        <div class="small muted">${doc.createdAt}</div>
                                    </td>
                                    <td><span class="badge badge--user">${doc.typeLabel}</span></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${doc.extractionStatus eq 'EXTRACTED'}">
                                                <span class="badge badge--success">${doc.extractedChars} chars</span>
                                                <div class="small muted">${doc.extractionMethod}</div>
                                            </c:when>
                                            <c:when test="${doc.extractionStatus eq 'PARTIAL'}">
                                                <span class="badge badge--warn">Partial</span>
                                                <div class="small muted">${doc.extractedChars} chars</div>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge badge--danger">No text</span>
                                                <c:if test="${not empty doc.extractionWarnings}">
                                                    <div class="small muted truncate" style="max-width:220px;" title="${fn:escapeXml(doc.extractionWarnings)}">${fn:escapeXml(doc.extractionWarnings)}</div>
                                                </c:if>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="num">${doc.sizeDisplay}</td>
                                    <td class="num nowrap">
                                        <c:if test="${doc.documentType eq 'SYLLABUS'}">
                                            <form method="post" action="${ctx}/syllabus/extract" style="display:inline;">
                                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                                <input type="hidden" name="examId" value="${exam.id}">
                                                <input type="hidden" name="documentId" value="${doc.id}">
                                                <button type="submit" class="btn btn--sm">Extract syllabus</button>
                                            </form>
                                        </c:if>
                                        <form method="post" action="${ctx}/documents/delete" style="display:inline;" data-confirm="Delete this document?">
                                            <input type="hidden" name="_csrf" value="${csrfToken}">
                                            <input type="hidden" name="documentId" value="${doc.id}">
                                            <button type="submit" class="btn btn--sm btn--danger">Delete</button>
                                        </form>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </section>

    <section class="section">
        <div class="section__head"><h2>Upload a file</h2></div>
        <div class="card">
            <form method="post" action="${ctx}/documents/upload" enctype="multipart/form-data">
                <input type="hidden" name="_csrf" value="${csrfToken}">
                <input type="hidden" name="examId" value="${exam.id}">
                <div class="field">
                    <label for="file">File</label>
                    <input type="file" id="file" name="file" required
                           accept=".pdf,.txt,.md,.docx,.pptx,.xlsx,.png,.jpg,.jpeg,.tif,.tiff">
                    <span class="help">PDF, Office, text and image files. Maximum ${uploadMaxMb} MB.</span>
                </div>
                <div class="field">
                    <label for="documentType">What is this?</label>
                    <select id="documentType" name="documentType">
                        <c:forEach var="t" items="${types}">
                            <option value="${t}">${fn:replace(t, '_', ' ')}</option>
                        </c:forEach>
                    </select>
                </div>
                <button type="submit" class="btn btn--primary btn--block" data-working-label="Uploading &amp; extracting…">Upload and extract</button>
            </form>
            <p class="help" style="margin-top:.6rem;">Files are stored outside the web root and are only ever served to their owner.</p>
        </div>
    </section>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
