<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/header.jsp" %>
<h1>My orders</h1><p><a href="${pageContext.request.contextPath}/refunds">Refund requests</a> Â· <a href="${pageContext.request.contextPath}/wallet">Reward points</a></p><label>Search orders<input id="orderSearch" class="form-control" placeholder="Order code, product or store"></label><script>document.addEventListener("DOMContentLoaded",()=>{document.getElementById("orderSearch").addEventListener("input",e=>{document.querySelectorAll("section.panel").forEach(p=>p.hidden=!p.textContent.toLowerCase().includes(e.target.value.toLowerCase()));});});</script>
<c:if test="${empty orders}"><p>You have no orders yet.</p></c:if>
<c:forEach items="${orders}" var="o"><section class="panel">
<h2><c:out value="${o.order_code}"/></h2><p>Status: <c:out value="${o.status}"/> Â· Payment method: <c:out value="${o.payment_method}"/></p>
<p><c:out value="${o.recipient_name} - ${o.delivery_address}"/></p>
<c:forEach items="${items}" var="i"><c:if test="${i.order_id == o.id}"><p><c:out value="${i.store_name} - ${i.title}"/> Ã— ${i.quantity} Â· <fmt:formatNumber value="${i.unit_price * i.quantity}"/> VND Â· <c:out value="${i.seller_status}"/> Â· Seller order #${i.seller_order_id}</p><p><c:out value="${i.rejection_reason}"/></p><c:if test="${i.seller_status == 'NEW' || i.seller_status == 'ACCEPTED'}"><form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="cancel"><input type="hidden" name="id" value="${i.seller_order_id}"><button class="button">Cancel this seller order</button></form></c:if></c:if></c:forEach>
<p>Delivery: <fmt:formatNumber value="${o.delivery_fee}"/> VND</p><p>Current payable after seller cancellations: <fmt:formatNumber value="${o.current_payable}"/> VND</p><strong>Original total: <fmt:formatNumber value="${o.payable_amount}"/> VND</strong>
<c:if test="${o.status == 'AWAITING_PAYMENT'}"><form method="post" action="${pageContext.request.contextPath}/payment/start"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${o.id}"><button class="button">Pay / retry with VNPay</button></form><p>Payment deadline: <c:out value="${o.payment_expires_at}"/> UTC</p></c:if>
<details><summary>Payment attempts</summary><c:forEach items="${payments}" var="p"><c:if test="${p.order_id == o.id}"><p>#${p.id} · <c:out value="${p.method}"/> · <fmt:formatNumber value="${p.amount}"/> VND · <c:out value="${p.status}"/> · <c:out value="${p.created_at}"/> UTC</p></c:if></c:forEach></details><p>Payment status: <c:out value="${o.payment_status}"/> Â· Estimated delivery: <c:out value="${o.estimated_delivery_at}"/> UTC</p>

</section></c:forEach>
<%@ include file="/WEB-INF/views/footer.jsp" %>
