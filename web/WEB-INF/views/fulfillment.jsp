<%@ page pageEncoding="UTF-8" %><%@ include file="/WEB-INF/views/header.jsp" %>
<h1>Order fulfillment</h1><c:if test="${sessionScope.user.role == 'SELLER'}"><form method="post" class="panel"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="pickupAddress"><label>Store pickup address<select name="address" required class="form-control"><c:forEach items="${pickupAddresses}" var="a"><option value="${a.id}"><c:out value="${a.address_line}, ${a.city}"/></option></c:forEach></select></label><button class="button">Set pickup address</button><p>Add addresses in My account.</p></form></c:if>
<p><a href="${pageContext.request.contextPath}/seller/promotions">Seller campaigns</a></p>
<c:if test="${empty sellerOrders}"><p>No orders to display.</p></c:if>
<c:forEach items="${sellerOrders}" var="o"><section class="panel">
<h2><c:out value="${o.order_code} - ${o.store_name}"/> (#${o.id})</h2>
<p>Status: <c:out value="${o.status}"/> · Payment: <c:out value="${o.payment_method}"/> · COD to collect: <fmt:formatNumber value="${o.cod_amount}"/> VND</p>
<p>Recipient: <c:out value="${o.recipient_name} - ${o.recipient_phone}"/></p><p>Destination: <c:out value="${o.delivery_address}"/></p>
<p>Pickup: <c:out value="${o.pickup_address}, ${o.pickup_city}"/></p><p>Cold-chain instructions: <c:out value="${o.cold_chain_note}"/></p>
<p>Rejection/failure: <c:out value="${o.rejection_reason} ${o.failure_reason}"/></p>
<c:forEach items="${allocations}" var="a"><c:if test="${a.seller_order_id == o.id}"><p><c:out value="${a.title} - ${a.option_name}"/> × ${a.quantity} · Batch <c:out value="${a.batch_no}"/> · Expiry <c:out value="${a.expiry_date}"/> · <c:out value="${a.reservation_status}"/></p></c:if></c:forEach>
<p>Subtotal: <fmt:formatNumber value="${o.subtotal}"/> · Discounts: <fmt:formatNumber value="${o.discount_amount + o.points_discount}"/> · Delivery: <fmt:formatNumber value="${o.delivery_fee}"/> VND</p>
<c:if test="${o.order_status == 'CONFIRMED'}">
<form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${o.id}">
<c:if test="${sessionScope.user.role == 'SELLER'}">
<c:if test="${o.status == 'NEW'}"><button name="action" value="accept" class="button">Accept order</button></c:if>
<c:if test="${o.status == 'NEW' || o.status == 'ACCEPTED'}"><label>Rejection reason<input class="form-control" name="detail" maxlength="500"></label><button name="action" value="reject" class="button">Reject order</button></c:if>
<c:if test="${o.status == 'ACCEPTED'}"><button name="action" value="ready" class="button">Packed and ready for delivery</button></c:if>
</c:if>
<c:if test="${sessionScope.user.role == 'ADMIN' && o.status == 'READY'}"><label>Courier<select name="courier" class="form-control"><c:forEach items="${couriers}" var="c"><option value="${c.id}"><c:out value="${c.full_name}"/></option></c:forEach></select></label><label>Cold-chain instructions<input name="detail" class="form-control" maxlength="255" required></label><button name="action" value="assign" class="button">Assign courier</button></c:if>
<c:if test="${sessionScope.user.role == 'DELIVERY_STAFF'}">
<c:choose><c:when test="${o.status == 'ASSIGNED'}"><button name="action" value="pickup" class="button">Confirm pickup</button></c:when><c:when test="${o.status == 'PICKED_UP'}"><button name="action" value="transit" class="button">In transit</button></c:when><c:when test="${o.status == 'IN_TRANSIT'}"><button name="action" value="arrive" class="button">Arriving</button></c:when></c:choose>
<c:if test="${o.status == 'ARRIVING'}"><label>Delivery evidence URL<input name="detail" class="form-control" maxlength="500"></label><label><input type="checkbox" name="collected"> COD collected in full (when applicable)</label><button name="action" value="deliver" class="button">Confirm delivered</button></c:if>
<c:if test="${o.status == 'ASSIGNED' || o.status == 'PICKED_UP' || o.status == 'IN_TRANSIT'}"><label>Failure reason<input name="detail" class="form-control" maxlength="500"></label></c:if>
<c:if test="${o.status == 'ASSIGNED' || o.status == 'PICKED_UP' || o.status == 'IN_TRANSIT' || o.status == 'ARRIVING'}"><button name="action" value="fail" class="button">Report delivery failure</button></c:if>
</c:if></form></c:if>
<p>Settlement: <c:out value="${o.settlement_status}"/> · Commission: <fmt:formatNumber value="${o.commission}"/> · Seller net: <fmt:formatNumber value="${o.net_payable}"/> VND</p>
<c:if test="${sessionScope.user.role == 'ADMIN' && o.status == 'DELIVERED' && o.settlement_status == 'UNSETTLED'}"><form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${o.id}"><button class="button" name="action" value="settle">Record eligible settlement</button></form></c:if>
</section></c:forEach>
<%@ include file="/WEB-INF/views/footer.jsp" %>
