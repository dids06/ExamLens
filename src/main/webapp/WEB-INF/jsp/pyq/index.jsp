<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Previous papers" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>Previous-year papers</h1>
        <p class="lede">Add past papers and let the AI surface recurring topics. Frequency counts come only from the questions it can actually extract.</p>
    </div>
    <div class="page-head__actions">
        <form method="post" action="${ctx}/pyq/analyse">
            <input type="hidden" name="_csrf" value="${csrfToken}">
            <input type="hidden" name="examId" value="${exam.id}">
            <button type="submit" class="btn btn--primary" data-working-label="Analysing…">Run analysis</button>
        </form>
    </div>
</div>

<div class="split">
    <div class="stack">
        <section class="section">
            <div class="section__head"><h2>Papers</h2><span class="hint">${fn:length(papers)} paper<c:if test="${fn:length(papers) ne 1}">s</c:if></span></div>
            <c:choose>
                <c:when test="${empty papers}">
                    <div class="empty"><h3>No papers added</h3><p class="muted">Add at least one previous-year paper, then run the analysis.</p></div>
                </c:when>
                <c:otherwise>
                    <div class="table-wrap">
                        <table class="table">
                            <thead><tr><th>Label</th><th>Year</th><th>Document</th><th class="num">Questions</th><th class="num">Marks</th><th></th></tr></thead>
                            <tbody>
                                <c:forEach var="paper" items="${papers}">
                                    <tr>
                                        <td>${fn:escapeXml(paper.paperLabel)}</td>
                                        <td>${empty paper.examYear ? '—' : paper.examYear}</td>
                                        <td class="small muted">${fn:escapeXml(paper.documentName)}</td>
                                        <td class="num">${paper.questionCount}</td>
                                        <td class="num">${empty paper.totalMarks ? '—' : paper.totalMarks}</td>
                                        <td class="num"><a class="btn btn--sm" href="${ctx}/pyq?examId=${exam.id}&amp;paperId=${paper.id}">Questions</a></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>

        <c:if test="${not empty selectedPaperId}">
            <section class="section">
                <div class="section__head"><h2>Extracted questions</h2></div>
                <c:choose>
                    <c:when test="${empty questions}">
                        <div class="empty"><p class="muted">No questions extracted for this paper yet.</p></div>
                    </c:when>
                    <c:otherwise>
                        <div class="card card--flush">
                            <div class="table-wrap">
                                <table class="table">
                                    <thead><tr><th>#</th><th>Question</th><th>Matched topic</th><th class="num">Marks</th></tr></thead>
                                    <tbody>
                                        <c:forEach var="q" items="${questions}">
                                            <tr>
                                                <td>${q.questionKey}</td>
                                                <td>${fn:escapeXml(q.questionText)}</td>
                                                <td>${empty q.matchedTopic ? '—' : fn:escapeXml(q.matchedTopic)}</td>
                                                <td class="num">${empty q.marks ? '—' : q.marks}</td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </c:if>
    </div>

    <div class="stack">
        <section class="section">
            <div class="section__head"><h2>Add a paper</h2></div>
            <div class="card">
                <c:choose>
                    <c:when test="${empty documents}">
                        <p class="muted small">Upload a past paper document first.</p>
                        <a class="btn btn--sm" href="${ctx}/documents?examId=${exam.id}">Upload a document</a>
                    </c:when>
                    <c:otherwise>
                        <form method="post" action="${ctx}/pyq/paper/add">
                            <input type="hidden" name="_csrf" value="${csrfToken}">
                            <input type="hidden" name="examId" value="${exam.id}">
                            <div class="field">
                                <label for="documentId">Document</label>
                                <select id="documentId" name="documentId">
                                    <c:forEach var="doc" items="${documents}">
                                        <option value="${doc.id}" ${doc.documentType eq 'PREVIOUS_PAPER' ? 'selected' : ''}>${fn:escapeXml(doc.originalFilename)}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="form-grid">
                                <div class="field"><label for="label">Label</label><input type="text" id="label" name="label" maxlength="180" placeholder="e.g. June 2023" required></div>
                                <div class="field"><label for="year">Year</label><input type="text" id="year" name="year" maxlength="12" placeholder="2023"></div>
                                <div class="field"><label for="totalMarks">Total marks</label><input type="number" id="totalMarks" name="totalMarks" min="1" max="10000"></div>
                            </div>
                            <button type="submit" class="btn btn--primary btn--block" data-working-label="Adding…">Add paper</button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>

        <c:if test="${not empty analysis}">
            <section class="section">
                <div class="section__head"><h2>Analysis</h2>
                    <c:if test="${analysis.simulated}"><span class="badge badge--warn">simulated</span></c:if>
                </div>
                <div class="card">
                    <c:if test="${not empty analysis.topicFrequencyList}">
                        <div class="table-wrap">
                            <table class="table">
                                <thead><tr><th>Topic</th><th class="num">Count</th><th class="num">Marks</th></tr></thead>
                                <tbody>
                                    <c:forEach var="f" items="${analysis.topicFrequencyList}">
                                        <tr>
                                            <td>${fn:escapeXml(f.topic)}
                                                <c:if test="${not empty f.years}"><div class="small muted"><c:forEach var="y" items="${f.years}" varStatus="ys">${fn:escapeXml(y)}<c:if test="${!ys.last}">, </c:if></c:forEach></div></c:if>
                                            </td>
                                            <td class="num">${f.observedCount}</td>
                                            <td class="num">${empty f.observedMarks ? '—' : f.observedMarks}</td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:if>
                    <c:if test="${not empty analysis.patternList}">
                        <h3 style="margin-top:.9rem;">Recurring patterns</h3>
                        <ul class="clean tick small"><c:forEach var="p" items="${analysis.patternList}"><li>${fn:escapeXml(p)}</li></c:forEach></ul>
                    </c:if>
                    <c:if test="${not empty analysis.unmatchedList}">
                        <h3 style="margin-top:.9rem;">Not matched to a topic</h3>
                        <ul class="clean small"><c:forEach var="u" items="${analysis.unmatchedList}"><li>${fn:escapeXml(u)}</li></c:forEach></ul>
                    </c:if>
                    <c:if test="${not empty analysis.limitationList}">
                        <h3 style="margin-top:.9rem;">Limitations</h3>
                        <ul class="clean cross small"><c:forEach var="l" items="${analysis.limitationList}"><li>${fn:escapeXml(l)}</li></c:forEach></ul>
                    </c:if>
                </div>
            </section>
        </c:if>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
