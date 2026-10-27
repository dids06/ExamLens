<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Syllabus" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>Syllabus</h1>
        <p class="lede">The AI reads your syllabus document and proposes topics with importance and difficulty. Nothing counts until you confirm it.</p>
    </div>
    <div class="page-head__actions">
        <span class="pill">${confirmedCount} confirmed topic<c:if test="${confirmedCount ne 1}">s</c:if></span>
    </div>
</div>

<div class="split">
    <div class="stack">
        <c:choose>
            <c:when test="${hasDraft}">
                <section class="section">
                    <div class="section__head">
                        <h2>Draft version ${draft.version.versionNumber}</h2>
                        <span class="badge badge--warn">draft · not confirmed</span>
                    </div>
                    <div class="card">
                        <c:if test="${not empty draft.version.summary}">
                            <p>${fn:escapeXml(draft.version.summary)}</p>
                        </c:if>
                        <c:if test="${not empty draft.version.warnings}">
                            <div class="flash flash--info"><div class="flash__body"><c:out value="${draft.version.warnings}" /></div></div>
                        </c:if>
                        <div class="form-actions">
                            <form method="post" action="${ctx}/syllabus/confirm">
                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                <input type="hidden" name="examId" value="${exam.id}">
                                <input type="hidden" name="versionId" value="${draft.version.id}">
                                <button type="submit" class="btn btn--primary" data-confirm="Confirm this syllabus? It will drive your plan and practice.">Confirm this syllabus</button>
                            </form>
                        </div>
                    </div>
                </section>

                <section class="section">
                    <div class="section__head"><h2>Proposed topics</h2><span class="hint">${fn:length(draft.topics)} topic<c:if test="${fn:length(draft.topics) ne 1}">s</c:if></span></div>
                    <c:choose>
                        <c:when test="${empty draft.topics}">
                            <div class="empty"><h3>No topics proposed</h3><p class="muted">The AI did not find topic statements in this document. Try another document or add topics manually.</p></div>
                        </c:when>
                        <c:otherwise>
                            <div class="stack">
                                <c:forEach var="topic" items="${draft.topics}">
                                    <div class="card">
                                        <div class="row row--between">
                                            <h3>${fn:escapeXml(topic.title)}</h3>
                                            <span class="row">
                                                <c:if test="${not empty topic.importance}"><span class="badge badge--ai">${fn:toLowerCase(topic.importance)}</span></c:if>
                                                <c:if test="${not empty topic.difficulty}"><span class="badge badge--muted">${fn:toLowerCase(topic.difficulty)}</span></c:if>
                                                <c:if test="${topic.needsReview}"><span class="badge badge--warn">review</span></c:if>
                                            </span>
                                        </div>
                                        <c:if test="${not empty topic.unitName}"><p class="small muted" style="margin:.2rem 0;">Unit: ${fn:escapeXml(topic.unitName)}</p></c:if>
                                        <c:if test="${not empty topic.aiReason}"><p class="small muted">${fn:escapeXml(topic.aiReason)}</p></c:if>
                                        <details class="disclosure">
                                            <summary>Edit topic</summary>
                                            <div class="disclosure__body">
                                                <form method="post" action="${ctx}/syllabus/topic/update">
                                                    <input type="hidden" name="_csrf" value="${csrfToken}">
                                                    <input type="hidden" name="examId" value="${exam.id}">
                                                    <input type="hidden" name="topicId" value="${topic.id}">
                                                    <div class="field"><label>Title</label><input type="text" name="title" value="${fn:escapeXml(topic.title)}" maxlength="255" required></div>
                                                    <div class="form-grid">
                                                        <div class="field"><label>Unit</label><input type="text" name="unitName" value="${fn:escapeXml(topic.unitName)}" maxlength="180"></div>
                                                        <div class="field"><label>Importance</label>
                                                            <select name="importance">
                                                                <option value="HIGH" ${topic.importance eq 'HIGH' ? 'selected' : ''}>High</option>
                                                                <option value="MEDIUM" ${topic.importance eq 'MEDIUM' ? 'selected' : ''}>Medium</option>
                                                                <option value="LOW" ${topic.importance eq 'LOW' ? 'selected' : ''}>Low</option>
                                                            </select>
                                                        </div>
                                                        <div class="field"><label>Difficulty</label>
                                                            <select name="difficulty">
                                                                <option value="EASY" ${topic.difficulty eq 'EASY' ? 'selected' : ''}>Easy</option>
                                                                <option value="MEDIUM" ${topic.difficulty eq 'MEDIUM' ? 'selected' : ''}>Medium</option>
                                                                <option value="HARD" ${topic.difficulty eq 'HARD' ? 'selected' : ''}>Hard</option>
                                                            </select>
                                                        </div>
                                                        <div class="field"><label>Estimated minutes</label><input type="number" name="estimatedMinutes" min="5" max="1440" value="${topic.estimatedMinutes}"></div>
                                                    </div>
                                                    <div class="field"><label>Subtopics (one per line)</label><textarea name="subtopics" maxlength="4000">${fn:escapeXml(topic.subtopics)}</textarea></div>
                                                    <div class="field"><label>Learning outcomes (one per line)</label><textarea name="learningOutcomes" maxlength="4000">${fn:escapeXml(topic.learningOutcomes)}</textarea></div>
                                                    <label class="check"><input type="checkbox" name="needsReview" ${topic.needsReview ? 'checked' : ''}> Still needs review</label>
                                                    <div class="form-actions">
                                                        <button type="submit" class="btn btn--primary btn--sm">Save topic</button>
                                                    </div>
                                                </form>
                                                <form method="post" action="${ctx}/syllabus/topic/delete" data-confirm="Remove this topic from the draft?" style="margin-top:.5rem;">
                                                    <input type="hidden" name="_csrf" value="${csrfToken}">
                                                    <input type="hidden" name="examId" value="${exam.id}">
                                                    <input type="hidden" name="topicId" value="${topic.id}">
                                                    <button type="submit" class="btn btn--danger btn--sm">Remove topic</button>
                                                </form>
                                            </div>
                                        </details>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>

                <section class="section">
                    <div class="section__head"><h2>Add a topic manually</h2></div>
                    <div class="card">
                        <form method="post" action="${ctx}/syllabus/topic/add">
                            <input type="hidden" name="_csrf" value="${csrfToken}">
                            <input type="hidden" name="examId" value="${exam.id}">
                            <div class="form-grid">
                                <div class="field"><label>Title</label><input type="text" name="title" maxlength="255" required></div>
                                <div class="field"><label>Unit</label><input type="text" name="unitName" maxlength="180"></div>
                                <div class="field"><label>Importance</label>
                                    <select name="importance"><option>MEDIUM</option><option>HIGH</option><option>LOW</option></select>
                                </div>
                                <div class="field"><label>Difficulty</label>
                                    <select name="difficulty"><option>MEDIUM</option><option>EASY</option><option>HARD</option></select>
                                </div>
                                <div class="field"><label>Estimated minutes</label><input type="number" name="estimatedMinutes" min="5" max="1440" placeholder="optional"></div>
                            </div>
                            <div class="form-actions"><button type="submit" class="btn btn--primary btn--sm">Add topic</button></div>
                        </form>
                    </div>
                </section>

                <c:if test="${not empty draft.classifications}">
                    <section class="section">
                        <div class="section__head"><h2>Line-by-line classification</h2><span class="hint">What the AI included or left out, and why</span></div>
                        <div class="table-wrap">
                            <table class="table">
                                <thead><tr><th>Source line</th><th>Decision</th><th>Reason</th></tr></thead>
                                <tbody>
                                    <c:forEach var="c" items="${draft.classifications}">
                                        <tr>
                                            <td style="max-width:360px;"><span class="truncate" style="display:block;">${fn:escapeXml(c.originalText)}</span>
                                                <span class="small muted"><c:if test="${not empty c.pageNumber}">page ${c.pageNumber}</c:if></span></td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${c.included}"><span class="badge badge--success">${c.classificationLabel}</span></c:when>
                                                    <c:otherwise><span class="badge badge--muted">${c.classificationLabel}</span></c:otherwise>
                                                </c:choose>
                                                <div class="small muted">${c.confidenceLabel}</div>
                                            </td>
                                            <td class="small">${fn:escapeXml(c.reason)}</td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </section>
                </c:if>
            </c:when>
            <c:otherwise>
                <div class="empty">
                    <h3>No syllabus draft yet</h3>
                    <p class="muted">Upload a syllabus document, then extract it to get a topic draft you can review.</p>
                    <a class="btn btn--primary" href="${ctx}/documents?examId=${exam.id}">Go to documents</a>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="stack">
        <section class="section">
            <div class="section__head"><h2>Extract from a document</h2></div>
            <div class="card">
                <c:choose>
                    <c:when test="${empty documents}">
                        <p class="muted small">No documents uploaded for this exam.</p>
                        <a class="btn btn--sm" href="${ctx}/documents?examId=${exam.id}">Upload a document</a>
                    </c:when>
                    <c:otherwise>
                        <form method="post" action="${ctx}/syllabus/extract">
                            <input type="hidden" name="_csrf" value="${csrfToken}">
                            <input type="hidden" name="examId" value="${exam.id}">
                            <div class="field">
                                <label for="documentId">Syllabus document</label>
                                <select id="documentId" name="documentId">
                                    <c:forEach var="doc" items="${documents}">
                                        <c:if test="${doc.documentType eq 'SYLLABUS'}">
                                            <option value="${doc.id}">${fn:escapeXml(doc.originalFilename)}</option>
                                        </c:if>
                                    </c:forEach>
                                </select>
                            </div>
                            <button type="submit" class="btn btn--primary btn--block" data-working-label="Extracting…">Run AI extraction</button>
                        </form>
                        <p class="help" style="margin-top:.5rem;">Each run creates a new draft version. Confirmed versions stay until you confirm a newer one.</p>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>

        <section class="section">
            <div class="section__head"><h2>Versions</h2></div>
            <div class="card">
                <c:choose>
                    <c:when test="${empty versions}"><p class="muted small">No versions yet.</p></c:when>
                    <c:otherwise>
                        <ul class="list">
                            <c:forEach var="v" items="${versions}">
                                <li class="list__item">
                                    <div class="row row--between">
                                        <strong>Version ${v.versionNumber}</strong>
                                        <c:choose>
                                            <c:when test="${v.confirmed}"><span class="badge badge--success">confirmed</span></c:when>
                                            <c:otherwise><span class="badge badge--warn">draft</span></c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div class="small muted">${v.createdAt}</div>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
