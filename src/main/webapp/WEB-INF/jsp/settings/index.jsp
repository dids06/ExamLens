<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Settings" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>Settings</h1>
        <p class="lede">Your account, study preferences and AI configuration.</p>
    </div>
</div>

<div class="split">
    <div class="stack">
        <section class="section">
            <div class="section__head"><h2>Profile</h2></div>
            <div class="card">
                <form method="post" action="${ctx}/settings/profile">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <div class="field"><label for="displayName">Name</label><input type="text" id="displayName" name="displayName" value="${fn:escapeXml(account.displayName)}" maxlength="120" required></div>
                    <div class="field"><label for="email">Email</label><input type="email" id="email" value="${fn:escapeXml(account.email)}" disabled><span class="help">Your email cannot be changed.</span></div>
                    <div class="form-grid">
                        <div class="field"><label for="timezone">Time zone</label>
                            <select id="timezone" name="timezone">
                                <c:forEach var="tz" items="${timezones}">
                                    <option value="${tz}" ${account.timezone eq tz ? 'selected' : ''}>${tz}</option>
                                </c:forEach>
                                <c:if test="${not empty account.timezone and not timezones.contains(account.timezone)}">
                                    <option value="${fn:escapeXml(account.timezone)}" selected>${fn:escapeXml(account.timezone)}</option>
                                </c:if>
                            </select>
                        </div>
                        <div class="field"><label for="theme">Theme</label>
                            <select id="theme" name="theme">
                                <option value="system" ${account.theme eq 'system' ? 'selected' : ''}>Match system</option>
                                <option value="light" ${account.theme eq 'light' ? 'selected' : ''}>Light</option>
                                <option value="dark" ${account.theme eq 'dark' ? 'selected' : ''}>Dark</option>
                            </select>
                        </div>
                    </div>
                    <div class="form-actions"><button type="submit" class="btn btn--primary" data-working-label="Saving…">Save profile</button></div>
                </form>
            </div>
        </section>

        <section class="section">
            <div class="section__head"><h2>Change password</h2></div>
            <div class="card">
                <form method="post" action="${ctx}/settings/password">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <div class="field"><label for="currentPassword">Current password</label><input type="password" id="currentPassword" name="currentPassword" autocomplete="current-password" required></div>
                    <div class="form-grid">
                        <div class="field"><label for="newPassword">New password</label><input type="password" id="newPassword" name="newPassword" autocomplete="new-password" required></div>
                        <div class="field"><label for="confirmPassword">Confirm new password</label><input type="password" id="confirmPassword" name="confirmPassword" autocomplete="new-password" required></div>
                    </div>
                    <div class="form-actions"><button type="submit" class="btn btn--primary" data-working-label="Updating…">Change password</button></div>
                </form>
            </div>
        </section>

        <section class="section">
            <div class="section__head"><h2>Study preferences</h2><span class="hint">Defaults the AI considers when planning</span></div>
            <div class="card">
                <form method="post" action="${ctx}/settings/study">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <div class="form-grid">
                        <div class="field"><label for="defaultStudyMinutes">Default daily study time (minutes)</label><input type="number" id="defaultStudyMinutes" name="defaultStudyMinutes" min="15" max="720" value="${study.defaultStudyMinutes}"></div>
                        <div class="field"><label for="preferredDepth">Preferred depth</label>
                            <select id="preferredDepth" name="preferredDepth">
                                <option value="BRIEF" ${study.preferredDepth eq 'BRIEF' ? 'selected' : ''}>Brief</option>
                                <option value="STANDARD" ${study.preferredDepth eq 'STANDARD' ? 'selected' : ''}>Standard</option>
                                <option value="DEEP" ${study.preferredDepth eq 'DEEP' ? 'selected' : ''}>Deep</option>
                            </select>
                        </div>
                        <div class="field"><label for="retentionDays">Retention window (days)</label><input type="number" id="retentionDays" name="retentionDays" min="30" max="3650" value="${study.retentionDays}"></div>
                    </div>
                    <div class="form-actions"><button type="submit" class="btn btn--primary" data-working-label="Saving…">Save preferences</button></div>
                </form>
            </div>
        </section>
    </div>

    <div class="stack">
        <section class="section">
            <div class="section__head"><h2>AI status</h2></div>
            <div class="card">
                <dl class="kv">
                    <dt>Provider</dt><dd>${fn:escapeXml(aiProvider)}</dd>
                    <dt>Model</dt><dd>${empty aiModel ? '—' : fn:escapeXml(aiModel)}</dd>
                    <dt>API key</dt>
                    <dd>
                        <c:choose>
                            <c:when test="${geminiConfigured}"><span class="badge badge--success">configured</span></c:when>
                            <c:otherwise><span class="badge badge--danger">not set</span></c:otherwise>
                        </c:choose>
                    </dd>
                </dl>
                <c:choose>
                    <c:when test="${aiSimulated}">
                        <p class="small muted" style="margin-top:.5rem;">ExamLens is running the built-in simulator. Its output is labelled <em>simulated</em> everywhere and is not from a live model.</p>
                    </c:when>
                    <c:when test="${not aiAvailable}">
                        <p class="small muted" style="margin-top:.5rem;">AI actions are disabled until <code>GEMINI_API_KEY</code> is set in the environment. ExamLens will not fabricate results in the meantime.</p>
                    </c:when>
                    <c:otherwise>
                        <p class="small muted" style="margin-top:.5rem;">AI actions will call the configured provider. Keys are read from the environment and never displayed.</p>
                    </c:otherwise>
                </c:choose>
                <details class="disclosure" style="margin-top:.6rem;">
                    <summary>Diagnostics</summary>
                    <div class="disclosure__body mono small">${fn:escapeXml(configSummary)}</div>
                </details>
            </div>
        </section>

        <section class="section">
            <div class="section__head"><h2>Delete account</h2></div>
            <div class="card">
                <p class="muted small">This permanently deletes your account and every exam, document, plan, paper and assessment you own. There is no undo.</p>
                <form method="post" action="${ctx}/settings/delete" data-confirm="Permanently delete your account and all data? This cannot be undone.">
                    <input type="hidden" name="_csrf" value="${csrfToken}">
                    <div class="field"><label for="confirmDelete">Type DELETE to confirm</label><input type="text" id="confirmDelete" name="confirmDelete" placeholder="DELETE" required></div>
                    <button type="submit" class="btn btn--danger">Delete my account</button>
                </form>
            </div>
        </section>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
