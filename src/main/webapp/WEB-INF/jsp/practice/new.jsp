<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="New practice paper" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>Generate practice</h1>
        <p class="lede">The AI chooses questions from your confirmed topics. You set the shape; it decides the content.</p>
    </div>
    <div class="page-head__actions"><a class="btn" href="${ctx}/practice?examId=${exam.id}">Back to practice</a></div>
</div>

<c:if test="${empty topics}">
    <div class="empty"><h3>Confirm your syllabus first</h3><p class="muted">Practice papers are generated from confirmed topics.</p>
        <a class="btn btn--primary" href="${ctx}/syllabus?examId=${exam.id}">Go to syllabus</a></div>
</c:if>

<c:if test="${not empty topics}">
    <div class="split">
        <section class="section">
            <div class="section__head"><h2>Mock paper</h2><span class="hint">A full-style paper with instructions</span></div>
            <div class="card">
                <form method="post" action="${ctx}/practice/generate">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <input type="hidden" name="examId" value="${exam.id}">
                    <div class="form-grid">
                        <div class="field"><label for="totalMarks">Total marks</label><input type="number" id="totalMarks" name="totalMarks" min="1" max="300" value="50"></div>
                        <div class="field"><label for="questionCount">Questions</label><input type="number" id="questionCount" name="questionCount" min="1" max="60" value="10"></div>
                        <div class="field"><label for="durationMinutes">Duration (minutes)</label><input type="number" id="durationMinutes" name="durationMinutes" min="5" max="600" placeholder="optional"></div>
                        <div class="field"><label for="difficulty">Difficulty</label>
                            <select id="difficulty" name="difficulty"><option>EASY</option><option selected>MEDIUM</option><option>HARD</option></select>
                        </div>
                    </div>
                    <fieldset>
                        <legend>Question types</legend>
                        <div class="grid grid--3">
                            <c:forEach var="t" items="${types}">
                                <label class="check"><input type="checkbox" name="types" value="${t}" ${t eq 'MCQ' or t eq 'SHORT_ANSWER' ? 'checked' : ''}> ${fn:replace(fn:toLowerCase(t), '_', ' ')}</label>
                            </c:forEach>
                        </div>
                    </fieldset>
                    <button type="submit" class="btn btn--primary" data-working-label="Generating…">Generate mock paper</button>
                </form>
            </div>
        </section>

        <section class="section">
            <div class="section__head"><h2>Focused practice set</h2><span class="hint">Target specific topics</span></div>
            <div class="card">
                <form method="post" action="${ctx}/practice/set">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <input type="hidden" name="examId" value="${exam.id}">
                    <div class="form-grid">
                        <div class="field"><label for="count">Questions</label><input type="number" id="count" name="count" min="1" max="40" value="10"></div>
                        <div class="field"><label for="difficulty2">Difficulty</label>
                            <select id="difficulty2" name="difficulty"><option>EASY</option><option selected>MEDIUM</option><option>HARD</option></select>
                        </div>
                    </div>
                    <fieldset>
                        <legend>Topics <span class="small muted">(leave all unchecked to use every confirmed topic)</span></legend>
                        <label class="check" style="margin-bottom:.4rem;"><input type="checkbox" data-check-all data-check-target="topicIds"> Select all</label>
                        <div class="stack" style="max-height:260px;overflow-y:auto;">
                            <c:forEach var="topic" items="${topics}">
                                <label class="check"><input type="checkbox" name="topicIds" value="${topic.id}"> ${fn:escapeXml(topic.title)}
                                    <c:if test="${not empty topic.importance}"><span class="badge badge--muted">${fn:toLowerCase(topic.importance)}</span></c:if>
                                </label>
                            </c:forEach>
                        </div>
                    </fieldset>
                    <fieldset>
                        <legend>Question types</legend>
                        <div class="grid grid--3">
                            <c:forEach var="t" items="${types}">
                                <label class="check"><input type="checkbox" name="types" value="${t}" ${t eq 'MCQ' or t eq 'SHORT_ANSWER' ? 'checked' : ''}> ${fn:replace(fn:toLowerCase(t), '_', ' ')}</label>
                            </c:forEach>
                        </div>
                    </fieldset>
                    <button type="submit" class="btn btn--primary" data-working-label="Generating…">Generate practice set</button>
                </form>
            </div>
        </section>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
