<%@ page pageEncoding="UTF-8" %><%@ include file="/WEB-INF/views/header.jsp" %>
<h1>Checkout</h1><p><a href="${pageContext.request.contextPath}/cart">Back to cart</a></p>
<form method="get" class="panel">
<label>Delivery address<select class="form-control" name="address" required><c:forEach items="${addresses}" var="a"><option value="${a.id}" ${addressId == a.id ? 'selected' : ''}><c:out value="${a.recipient_name} - ${a.address_line}, ${a.city}"/></option></c:forEach></select></label>
<label>Voucher code<input name="voucher" class="form-control" maxlength="30"></label><label>Reward points<input type="number" name="points" value="0" min="0" class="form-control"></label>
<label>Payment method<select name="method" class="form-control"><option value="COD">Cash on delivery</option><option value="CARD">Card via VNPay sandbox</option><option value="E_WALLET">VNPay QR / supported wallet</option></select></label><button class="button">Review order</button></form>
<c:if test="${empty addresses}"><p>Add a delivery address in <a href="${pageContext.request.contextPath}/account">My account</a> first.</p></c:if>
<c:if test="${not empty quote}"><section class="panel"><h2>Order summary</h2><p><c:out value="${quote.address.recipient_name} - ${quote.address.phone}"/></p><p><c:out value="${quote.address.address_line}, ${quote.address.ward}, ${quote.address.district}, ${quote.address.city}"/></p>
<c:forEach items="${quote.lines}" var="l"><p><c:out value="${l.store_name} - ${l.title}"/> x ${l.quantity}: <fmt:formatNumber value="${l.price * l.quantity}"/> VND</p></c:forEach>
<h3>Seller totals</h3><c:forEach items="${quote.groups}" var="g"><p><c:out value="${g.value.storeName}"/>: products <fmt:formatNumber value="${g.value.subtotal}"/> - discounts <fmt:formatNumber value="${g.value.discount + g.value.pointsDiscount}"/> + delivery <fmt:formatNumber value="${g.value.fee}"/> VND</p></c:forEach>
<p>Items: <fmt:formatNumber value="${quote.subtotal}"/> VND</p><p>Delivery: <fmt:formatNumber value="${quote.fee}"/> VND</p><p>Promotion savings: <fmt:formatNumber value="${quote.discount}"/> VND</p><p>Points discount: <fmt:formatNumber value="${quote.pointsDiscount}"/> VND</p><p>Estimated delivery: within ${quote.etaHours} hours after confirmation.</p>
<h3>Total: <fmt:formatNumber value="${quote.total}"/> VND</h3><p>Payment method: <c:out value="${quote.method}"/></p>
<form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="key" value="${checkoutKey}"><button class="button">Place order</button></form></section></c:if>
<%@ include file="/WEB-INF/views/footer.jsp" %>
