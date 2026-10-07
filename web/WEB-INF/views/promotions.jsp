<%@ page pageEncoding="UTF-8" %><%@ include file="/WEB-INF/views/header.jsp" %>
<h1>Promotions</h1><p>Schedule times use Vietnam time (UTC+7). Amounts are in VND.</p>
<form method="post" class="panel"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="save">
<label>Promotion ID (leave blank to create)<input name="id" type="number" min="1" class="form-control"></label>
<label>Name<input name="name" maxlength="150" required class="form-control"></label>
<c:choose><c:when test="${sessionScope.user.role == 'ADMIN'}"><label>Voucher code<input name="code" maxlength="30" required class="form-control"></label></c:when><c:otherwise><label>Eligible listing IDs (comma separated)<input name="listings" required class="form-control"></label></c:otherwise></c:choose>
<label>Discount type<select name="discountType" class="form-control"><option value="PERCENT">Percentage</option><option value="AMOUNT">Fixed amount</option></select></label>
<label>Discount value<input type="number" step="0.01" min="0.01" name="value" required class="form-control"></label>
<label>Minimum spend<input type="number" name="minimum" min="0" value="0" required class="form-control"></label>
<label>Total budget<input type="number" name="budget" min="1" required class="form-control"></label>
<label>Total uses<input type="number" name="limit" min="1" max="10000" required class="form-control"></label>
<label>Uses per customer<input type="number" name="perCustomer" min="1" max="10000" value="1" required class="form-control"></label>
<label>Start<input type="datetime-local" name="start" required class="form-control"></label><label>End<input type="datetime-local" name="end" required class="form-control"></label><button class="button">Save draft</button></form>
<c:forEach items="${promotions}" var="p"><section class="panel"><h2>#${p.id} <c:out value="${p.name}"/></h2><p><c:out value="${p.code}"/> · <c:out value="${p.status}"/> · <c:out value="${p.discount_type}"/> ${p.discount_value}</p><p>Redemptions: ${p.redemptions} · Budget used: <fmt:formatNumber value="${p.budget_used}"/> / <fmt:formatNumber value="${p.budget_limit}"/> VND</p><form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${p.id}"><input type="hidden" name="action" value="state"><select name="state"><option value="SCHEDULED">Publish / schedule</option><option value="PAUSED">Pause</option><option value="ENDED">End</option></select><button class="button">Update state</button></form><h3>Redemption history</h3><c:forEach items="${p.history}" var="h"><p><c:out value="${h.order_code}"/> · <fmt:formatNumber value="${h.discount_amount}"/> VND · <c:out value="${h.status}"/> · <c:out value="${h.created_at}"/> UTC</p></c:forEach></section></c:forEach>
<%@ include file="/WEB-INF/views/footer.jsp" %>
