<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/header.jsp" %>
<h1>Đơn hàng của tôi</h1><p><a href="${pageContext.request.contextPath}/refunds">Yêu cầu hoàn tiền</a> · <a href="${pageContext.request.contextPath}/wallet">Điểm thưởng</a></p><label>Tìm đơn hàng<input id="orderSearch" class="form-control" placeholder="Mã đơn, sản phẩm hoặc cửa hàng"></label><script>document.addEventListener("DOMContentLoaded", () => {document.getElementById("orderSearch").addEventListener("input", e => {document.querySelectorAll("section.panel").forEach(p => p.hidden = !p.textContent.toLowerCase().includes(e.target.value.toLowerCase())); }); });</script>
<c:if test="${empty orders}"><p>Bạn chưa có đơn hàng nào.</p></c:if>
<c:forEach items="${orders}" var="o"><section class="panel">
        <h2><c:out value="${o.orderCode}"/></h2><p>Trạng thái: <c:out value="${vi:label(o.status)}"/> · Phương thức thanh toán: <c:out value="${vi:label(o.paymentMethod)}"/></p>
        <p><c:out value="${o.recipientName} - ${o.deliveryAddress}"/></p>
        <c:forEach items="${items}" var="i"><c:if test="${i.orderId == o.id}"><p><c:out value="${i.storeName} - ${i.title}"/> × ${i.quantity} · <fmt:formatNumber value="${i.unitPrice * i.quantity}"/> đ · <c:out value="${vi:label(i.sellerStatus)}"/> · Đơn cửa hàng #${i.sellerOrderId}</p><p><c:out value="${i.rejectionReason}"/></p><%-- ThanhNC BA-26: only for a Confirmed order still within its deadline; the page's status is sent for MSG07. --%><c:if test="${o.status == 'CONFIRMED' && o.cancellable == 1 && (i.sellerStatus == 'NEW' || i.sellerStatus == 'ACCEPTED')}"><form method="post" data-confirm="${fn:escapeXml(sellerCancelConfirms[i.sellerOrderId])}"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="cancel"><input type="hidden" name="id" value="${i.sellerOrderId}"><input type="hidden" name="status" value="${i.sellerStatus}"><button class="button">Hủy đơn của cửa hàng này</button></form></c:if></c:if></c:forEach>
        <p>Phí giao hàng: <fmt:formatNumber value="${o.deliveryFee}"/> đ</p><p>Số tiền phải trả sau khi hủy đơn cửa hàng: <fmt:formatNumber value="${o.currentPayable}"/> đ</p><strong>Tổng tiền ban đầu: <fmt:formatNumber value="${o.payableAmount}"/> đ</strong>
        <c:if test="${o.status == 'CONFIRMED' && not empty cancelDeadlines[o.id]}"><p>Hạn hủy: <c:out value="${cancelDeadlines[o.id]}"/><c:if test="${o.cancellable != 1}"> (đã hết hạn hủy)</c:if></p></c:if>
        <%-- ThanhNC BA-20: an order awaiting online payment can only be cancelled as a whole. --%>
        <c:if test="${o.status == 'AWAITING_PAYMENT'}"><form method="post" data-confirm="${fn:escapeXml(orderCancelConfirms[o.id])}"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="cancelOrder"><input type="hidden" name="id" value="${o.id}"><button class="button">Hủy đơn hàng</button></form></c:if>
        <c:if test="${o.status == 'AWAITING_PAYMENT'}"><form method="post" action="${pageContext.request.contextPath}/payment/start"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${o.id}"><button class="button">Thanh toán / thử lại với VNPay</button></form><p>Hạn thanh toán: <c:out value="${o.paymentExpiresAt}"/> giờ quốc tế</p></c:if>
        <details><summary>Lịch sử thử thanh toán</summary><c:forEach items="${payments}" var="p"><c:if test="${p.orderId == o.id}"><p>#${p.id} · <c:out value="${vi:label(p.method)}"/> · <fmt:formatNumber value="${p.amount}"/> đ · <c:out value="${vi:label(p.status)}"/> · <c:out value="${p.createdAt}"/> giờ quốc tế</p></c:if></c:forEach></details><p>Trạng thái thanh toán: <c:out value="${vi:label(o.paymentStatus)}"/> · Dự kiến giao hàng: <c:out value="${o.estimatedDeliveryAt}"/> giờ quốc tế</p>

            </section></c:forEach>
<script>
    // ThanhNC BA-26: MSG06 confirmation before a cancellation (same pattern as account.jsp).
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (!window.confirm(form.getAttribute('data-confirm'))) { e.preventDefault(); }
        });
    });
</script>
<%@ include file="/WEB-INF/views/footer.jsp" %>
