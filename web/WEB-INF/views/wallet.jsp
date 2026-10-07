<%@ page pageEncoding="UTF-8" %><%@ include file="/WEB-INF/views/header.jsp" %>
<h1>Reward points</h1><h2>Available: ${wallet.points_balance}</h2><p>Pending: ${wallet.totals.pending} · Expired/reversed: ${wallet.totals.expired}</p><p>Membership: <c:out value="${wallet.tier}"/></p><p><c:out value="${wallet.benefits}"/></p><p>Points are credited after eligible delivery and can be redeemed at checkout.</p>
<p>Qualifying delivered spending over the last ${wallet.membership.periodDays} days: ${wallet.membership.qualifyingSpend} VND. Refunded orders are excluded.</p>
<c:if test="${not empty wallet.membership.warning}"><p class="alert alert-warning"><c:out value="${wallet.membership.warning}"/></p></c:if>
<c:if test="${not empty wallet.membership.nextTier}"><p>Next tier: <c:out value="${wallet.membership.nextTier}"/>. Remaining qualifying spending: ${wallet.membership.remainingSpend} VND.</p></c:if>
<table class="table"><thead><tr><th>Date</th><th>Type</th><th>Points</th><th>Remaining</th><th>Status</th><th>Expires</th></tr></thead><tbody><c:forEach items="${wallet.entries}" var="e"><tr><td><c:out value="${e.created_at}"/></td><td><c:out value="${e.type}"/></td><td>${e.points}</td><td>${e.remaining_points}</td><td><c:out value="${e.status}"/></td><td><c:out value="${e.points_expire_at}"/></td></tr></c:forEach></tbody></table>
<%@ include file="/WEB-INF/views/footer.jsp" %>
