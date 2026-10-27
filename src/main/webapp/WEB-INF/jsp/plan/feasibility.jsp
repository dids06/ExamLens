<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Feasibility" />
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<div class="page-head">
    <div class="page-head__text">
        <h1>Feasibility check</h1>
        <p class="lede">An honest read on whether the remaining work fits the time you have.</p>
    </div>
    <div class="page-head__actions"><a class="btn" href="${ctx}/plan?examId=${exam.id}">Back to plan</a></div>
</div>

<div class="panel panel--ai section">
    <div class="row row--between">
        <span class="badge badge--ai">AI assessment</span>
        <c:if test="${simulated}"><span class="badge badge--warn">simulated</span></c:if>
    </div>
    <p style="margin:.6rem 0 0;"><strong>${empty feasibility.feasibility ? 'Not assessed' : fn:replace(fn:toLowerCase(feasibility.feasibility), '_', ' ')}</strong></p>
    <c:if test="${not empty feasibility.notes}"><p>${fn:escapeXml(feasibility.notes)}</p></c:if>
</div>

<div class="grid grid--3 section">
    <div class="card stat"><div class="stat__label">Available</div><div class="stat__value" style="font-size:1.3rem;">${empty feasibility.availableMinutes ? '—' : feasibility.availableMinutes}</div><div class="stat__sub">minutes</div></div>
    <div class="card stat"><div class="stat__label">Required</div><div class="stat__value" style="font-size:1.3rem;">${empty feasibility.estimatedRequiredMinutes ? '—' : feasibility.estimatedRequiredMinutes}</div><div class="stat__sub">minutes</div></div>
    <div class="card stat"><div class="stat__label">Buffer</div><div class="stat__value" style="font-size:1.3rem;">${empty feasibility.bufferMinutes ? '—' : feasibility.bufferMinutes}</div><div class="stat__sub">minutes spare</div></div>
</div>

<c:if test="${not empty feasibility.highestPriorityTradeoffs}">
    <section class="section">
        <div class="section__head"><h2>Trade-offs to consider</h2></div>
        <div class="card"><ul class="clean small"><c:forEach var="t" items="${feasibility.highestPriorityTradeoffs}"><li>${fn:escapeXml(t)}</li></c:forEach></ul></div>
    </section>
</c:if>
<c:if test="${not empty feasibility.assumptions}">
    <section class="section">
        <div class="section__head"><h2>Assumptions</h2></div>
        <div class="card"><ul class="clean small"><c:forEach var="a" items="${feasibility.assumptions}"><li class="muted">${fn:escapeXml(a)}</li></c:forEach></ul></div>
    </section>
</c:if>

<%@ include file="/WEB-INF/jsp/fragments/foot.jspf" %>
