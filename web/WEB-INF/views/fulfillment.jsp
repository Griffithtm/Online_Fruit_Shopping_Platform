<%@ page pageEncoding="UTF-8" %><%@ include file="/WEB-INF/views/header.jsp" %>
<h1>Xử lý đơn hàng</h1><c:if test="${sessionScope.user.role == 'SELLER'}"><form method="post" class="panel"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="pickupAddress"><label>Địa chỉ lấy hàng của cửa hàng<select name="address" required class="form-control"><c:forEach items="${pickupAddresses}" var="a"><option value="${a.id}"><c:out value="${a.addressLine}, ${a.city}"/></option></c:forEach></select></label><button class="button">Lưu địa chỉ lấy hàng</button><p>Thêm địa chỉ tại Tài khoản của tôi.</p></form></c:if>
<c:if test="${sessionScope.user.role == 'SELLER'}"><p><a href="${pageContext.request.contextPath}/seller/promotions">Khuyến mãi cửa hàng</a></p></c:if>
<c:if test="${empty sellerOrders}"><p>Chưa có đơn hàng để hiển thị.</p></c:if>
<c:forEach items="${sellerOrders}" var="o"><section class="panel">
        <h2><c:out value="${o.orderCode} - ${o.storeName}"/> (#${o.id})</h2>
        <p>Trạng thái: <c:out value="${vi:label(o.status)}"/> · Thanh toán: <c:out value="${vi:label(o.paymentMethod)}"/> · Tiền cần thu khi giao: <fmt:formatNumber value="${o.codAmount}"/> đ</p>
        <p>Người nhận: <c:out value="${o.recipientName} - ${o.recipientPhone}"/></p><p>Địa chỉ giao: <c:out value="${o.deliveryAddress}"/></p>
        <p>Địa chỉ lấy hàng: <c:out value="${o.pickupAddress}, ${o.pickupCity}"/></p><p>Hướng dẫn bảo quản lạnh: <c:out value="${o.coldChainNote}"/></p>
        <p>Lý do từ chối/thất bại: <c:out value="${o.rejectionReason} ${o.failureReason}"/></p>
        <c:forEach items="${allocations}" var="a"><c:if test="${a.sellerOrderId == o.id}"><p><c:out value="${a.title} - ${a.optionName}"/> × ${a.quantity} · Lô <c:out value="${a.batchNo}"/> · Hạn sử dụng <c:out value="${a.expiryDate}"/> · <c:out value="${vi:label(a.reservationStatus)}"/></p></c:if></c:forEach>
        <p>Tổng tạm tính: <fmt:formatNumber value="${o.subtotal}"/> · Giảm giá: <fmt:formatNumber value="${o.discountAmount + o.pointsDiscount}"/> · Phí giao hàng: <fmt:formatNumber value="${o.deliveryFee}"/> đ</p>
        <c:if test="${o.orderStatus == 'CONFIRMED'}">
            <form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${o.id}">
                <c:if test="${sessionScope.user.role == 'SELLER'}">
                    <c:if test="${o.status == 'NEW'}"><button name="action" value="accept" class="button">Nhận đơn</button></c:if>
                    <c:if test="${o.status == 'NEW' || o.status == 'ACCEPTED'}"><label>Lý do từ chối<input class="form-control" name="detail" maxlength="500"></label><button name="action" value="reject" class="button">Từ chối đơn</button></c:if>
                    <c:if test="${o.status == 'ACCEPTED'}"><button name="action" value="ready" class="button">Đã đóng gói, sẵn sàng giao</button></c:if>
                </c:if>
                <c:if test="${sessionScope.user.role == 'ADMIN' && o.status == 'READY'}"><label>Nhân viên giao hàng<select name="courier" class="form-control"><c:forEach items="${couriers}" var="c"><option value="${c.id}"><c:out value="${c.fullName}"/></option></c:forEach></select></label><label>Hướng dẫn bảo quản lạnh<input name="detail" class="form-control" maxlength="255" required></label><button name="action" value="assign" class="button">Phân công giao hàng</button></c:if>
                        <c:if test="${sessionScope.user.role == 'DELIVERY_STAFF'}">
                                    <c:choose><c:when test="${o.status == 'ASSIGNED'}"><button name="action" value="pickup" class="button">Xác nhận đã lấy hàng</button></c:when><c:when test="${o.status == 'PICKED_UP'}"><button name="action" value="transit" class="button">Đang vận chuyển</button></c:when><c:when test="${o.status == 'IN_TRANSIT'}"><button name="action" value="arrive" class="button">Sắp giao đến</button></c:when></c:choose>
                    <c:if test="${o.status == 'ARRIVING'}"><label>Đường dẫn bằng chứng giao hàng<input name="detail" class="form-control" maxlength="500"></label><label><input type="checkbox" name="collected"> Đã thu đủ tiền khi giao (nếu áp dụng)</label><button name="action" value="deliver" class="button">Xác nhận đã giao</button></c:if>
                    <c:if test="${o.status == 'ASSIGNED' || o.status == 'PICKED_UP' || o.status == 'IN_TRANSIT'}"><label>Lý do giao thất bại<input name="detail" class="form-control" maxlength="500"></label></c:if>
                    <c:if test="${o.status == 'ASSIGNED' || o.status == 'PICKED_UP' || o.status == 'IN_TRANSIT' || o.status == 'ARRIVING'}"><button name="action" value="fail" class="button">Báo giao hàng thất bại</button></c:if>
                </c:if></form></c:if>
        <p>Quyết toán: <c:out value="${vi:label(o.settlementStatus)}"/> · Hoa hồng: <fmt:formatNumber value="${o.commission}"/> · Thực nhận của người bán: <fmt:formatNumber value="${o.netPayable}"/> đ</p>
        <c:if test="${sessionScope.user.role == 'ADMIN' && o.status == 'DELIVERED' && o.settlementStatus == 'UNSETTLED'}"><form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${o.id}"><button class="button" name="action" value="settle">Ghi nhận quyết toán đủ điều kiện</button></form></c:if>
    </section></c:forEach>
<%@ include file="/WEB-INF/views/footer.jsp" %>
