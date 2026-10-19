<%@ page pageEncoding="UTF-8" %><%@ include file="/WEB-INF/views/header.jsp" %>
<h1>Xử lý đơn hàng</h1><c:if test="${sessionScope.user.role == 'SELLER'}"><form method="post" class="panel"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="pickupAddress"><label>Địa chỉ lấy hàng của cửa hàng<select name="address" required class="form-control"><c:forEach items="${pickupAddresses}" var="a"><option value="${a.id}"><c:out value="${a.addressLine}, ${a.city}"/></option></c:forEach></select></label><button class="button">Lưu địa chỉ lấy hàng</button><p>Thêm địa chỉ tại Tài khoản của tôi.</p></form></c:if>
<c:if test="${sessionScope.user.role == 'SELLER'}"><p><a href="${pageContext.request.contextPath}/seller/promotions">Khuyến mãi cửa hàng</a></p>
    <%-- ThanhNC BA-33: status tabs (server-side filter). --%>
    <nav aria-label="Lọc theo trạng thái" class="d-flex flex-wrap gap-2 mb-3">
        <c:forEach items="${['', 'NEW', 'ACCEPTED', 'READY', 'SHIPPING', 'DELIVERED', 'CLOSED', 'FAILED']}" var="t" varStatus="st">
            <c:set var="tabLabel" value="${['Tất cả', 'Đơn mới', 'Đã tiếp nhận', 'Sẵn sàng giao', 'Đang giao', 'Đã giao', 'Đã hủy/từ chối', 'Giao thất bại'][st.index]}" />
            <a class="button ${sellerTab == t ? 'active' : ''}" ${sellerTab == t ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/seller/orders?status=${t}"><c:out value="${tabLabel}"/></a>
        </c:forEach>
    </nav>
</c:if>
<%-- ThanhNC BA-31: seller orders whose "ready" was blocked by expired/unavailable batches in the last 24 hours. --%>
<c:if test="${sessionScope.user.role == 'ADMIN' && not empty readyBlocked}"><section class="panel" role="status"><h2>Đơn bị chặn chuyển sang sẵn sàng giao (24 giờ qua)</h2>
    <c:forEach items="${readyBlocked}" var="b"><p><c:out value="${b.note}"/></p></c:forEach></section></c:if>
<c:if test="${empty sellerOrders}"><p>Chưa có đơn hàng để hiển thị.</p></c:if>
<c:forEach items="${sellerOrders}" var="o"><section class="panel">
        <h2><c:out value="${o.orderCode}-${o.storeId} · ${o.storeName}"/></h2>
        <%-- ThanhNC BA-33: unpaid online orders are visible to the Seller but have no actions yet. --%>
        <c:if test="${sessionScope.user.role == 'SELLER' && o.orderStatus == 'AWAITING_PAYMENT'}"><p><strong>Chờ khách thanh toán</strong></p></c:if>
        <p>Trạng thái: <c:out value="${vi:label(o.status)}"/> · Thanh toán: <c:out value="${vi:label(o.paymentMethod)}"/> · Tiền cần thu khi giao: <fmt:formatNumber value="${o.codAmount}"/> đ</p>
        <c:choose>
            <%-- ThanhNC BA-34: the Seller sees only the recipient name and province/city (the query does not load phone or address). --%>
            <c:when test="${sessionScope.user.role == 'SELLER'}"><p>Người nhận: <c:out value="${o.recipientName}"/> · Tỉnh/thành: <c:out value="${o.deliveryCity}"/></p></c:when>
            <c:otherwise><p>Người nhận: <c:out value="${o.recipientName} - ${o.recipientPhone}"/></p><p>Địa chỉ giao: <c:out value="${o.deliveryAddress}"/></p></c:otherwise>
        </c:choose>
        <p>Địa chỉ lấy hàng: <c:out value="${o.pickupAddress}, ${o.pickupCity}"/></p>
        <%-- ThanhNC BA-32: the Seller's storage note and the Admin's cold-chain instructions are both shown. --%>
        <c:if test="${not empty o.packingNote}"><p>Ghi chú bảo quản của người bán: <c:out value="${o.packingNote}"/></p></c:if>
        <p>Hướng dẫn bảo quản lạnh (quản trị viên): <c:out value="${o.coldChainNote}"/></p>
        <p>Lý do từ chối/thất bại: <c:out value="${o.rejectionReason} ${o.failureReason}"/></p>
        <c:forEach items="${allocations}" var="a"><c:if test="${a.sellerOrderId == o.id}"><p><c:out value="${a.title} - ${a.optionName}"/> × ${a.quantity} · Lô <c:out value="${a.batchNo}"/> · Hạn sử dụng <c:out value="${a.expiryDate}"/> · <c:out value="${vi:label(a.reservationStatus)}"/></p></c:if></c:forEach>
        <p>Tổng tạm tính: <fmt:formatNumber value="${o.subtotal}"/> · Giảm giá: <fmt:formatNumber value="${o.discountAmount + o.pointsDiscount}"/> · Phí giao hàng: <fmt:formatNumber value="${o.deliveryFee}"/> đ</p>
        <c:if test="${o.orderStatus == 'CONFIRMED'}">
            <form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${o.id}">
                <c:if test="${sessionScope.user.role == 'SELLER'}">
                    <c:if test="${o.status == 'NEW'}"><button name="action" value="accept" class="button">Nhận đơn</button></c:if>
                    <%-- ThanhNC BA-29: rejection only while New. --%>
                    <c:if test="${o.status == 'NEW'}"><label>Lý do từ chối<input class="form-control" name="detail" maxlength="500"></label><button name="action" value="reject" class="button">Từ chối đơn</button></c:if>
                    <%-- ThanhNC BA-32: packing confirmation (required) and storage note. --%>
                    <c:if test="${o.status == 'ACCEPTED'}"><label><input type="checkbox" name="packed" required> Đã đóng gói</label><label>Ghi chú bảo quản (không bắt buộc, tối đa 255 ký tự)<input class="form-control" name="packingNote" maxlength="255"></label><button name="action" value="ready" class="button">Đã đóng gói, sẵn sàng giao</button></c:if>
                </c:if>
                <c:if test="${sessionScope.user.role == 'ADMIN' && o.status == 'READY'}"><label>Nhân viên giao hàng<select name="courier" class="form-control"><c:forEach items="${couriers}" var="c"><option value="${c.id}"><c:out value="${c.fullName}"/></option></c:forEach></select></label><label>Hướng dẫn bảo quản lạnh<input name="detail" class="form-control" maxlength="255" required></label><button name="action" value="assign" class="button">Phân công giao hàng</button></c:if>
                </form>
            <%-- ThanhNC BA-35/BA-37/BA-38: the courier has separate forms (status step, delivery with photo and COD amount, failure with its own reason). --%>
            <c:if test="${sessionScope.user.role == 'DELIVERY_STAFF'}">
                <c:if test="${o.status == 'ASSIGNED' || o.status == 'PICKED_UP' || o.status == 'IN_TRANSIT'}">
                    <form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${o.id}">
                        <c:choose><c:when test="${o.status == 'ASSIGNED'}"><button name="action" value="pickup" class="button">Xác nhận đã lấy hàng</button></c:when><c:when test="${o.status == 'PICKED_UP'}"><button name="action" value="transit" class="button">Đang vận chuyển</button></c:when><c:otherwise><button name="action" value="arrive" class="button">Sắp giao đến</button></c:otherwise></c:choose>
                    </form>
                </c:if>
                <c:if test="${o.status == 'ARRIVING'}">
                    <form method="post" enctype="multipart/form-data" class="panel" data-evidence-max="5242880" data-evidence-message="${fn:escapeXml(evidenceTooLarge)}"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${o.id}"><input type="hidden" name="action" value="deliver">
                        <h3>Xác nhận đã giao</h3>
                        <label>Ảnh bằng chứng giao hàng (JPG/PNG, tối đa 5 MB)${o.evidenceRequired ? ' – bắt buộc' : ' – không bắt buộc'}
                            <input type="file" name="evidence" class="form-control" accept="image/png,image/jpeg" ${o.evidenceRequired ? 'required' : ''}></label>
                        <c:if test="${o.paymentMethod == 'COD'}">
                            <label>Số tiền đã thu (phải thu đúng <fmt:formatNumber value="${o.codAmount}"/> đ)
                                <input type="number" name="codAmount" class="form-control" min="0" step="1" required></label>
                        </c:if>
                        <button class="button">Xác nhận đã giao</button>
                    </form>
                </c:if>
                <c:if test="${o.status == 'ASSIGNED' || o.status == 'PICKED_UP' || o.status == 'IN_TRANSIT' || o.status == 'ARRIVING'}">
                    <form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${o.id}"><input type="hidden" name="action" value="fail">
                        <label>Lý do giao thất bại<input name="failureReason" class="form-control" maxlength="500" required></label>
                        <button class="button">Báo giao hàng thất bại</button>
                    </form>
                </c:if>
            </c:if></c:if>
        <c:if test="${not empty o.evidenceUrl && (sessionScope.user.role == 'ADMIN' || sessionScope.user.role == 'DELIVERY_STAFF')}"><p><a href="${pageContext.request.contextPath}${o.evidenceUrl}" target="_blank" rel="noopener">Xem ảnh bằng chứng giao hàng</a></p></c:if>
        <%-- ThanhNC BA-51: settlement data is not shown to couriers (and not loaded for them). --%>
        <c:if test="${sessionScope.user.role != 'DELIVERY_STAFF'}">
        <p>Quyết toán: <c:out value="${vi:label(o.settlementStatus)}"/> · Hoa hồng: <fmt:formatNumber value="${o.commission}"/> · Thực nhận của người bán: <fmt:formatNumber value="${o.netPayable}"/> đ</p>
        </c:if>
        <c:if test="${sessionScope.user.role == 'ADMIN' && o.status == 'DELIVERED' && o.settlementStatus == 'UNSETTLED'}"><form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${o.id}"><button class="button" name="action" value="settle">Ghi nhận quyết toán đủ điều kiện</button></form></c:if>
    </section></c:forEach>
<%-- ThanhNC BA-33: paging for the Seller list. --%>
<c:if test="${sessionScope.user.role == 'SELLER' && pages > 1}"><nav aria-label="Phân trang" class="d-flex gap-2 align-items-center">
    <c:if test="${page > 1}"><a class="button" href="${pageContext.request.contextPath}/seller/orders?status=${sellerTab}&page=${page - 1}">Trang trước</a></c:if>
    <span>Trang ${page}/${pages}</span>
    <c:if test="${page < pages}"><a class="button" href="${pageContext.request.contextPath}/seller/orders?status=${sellerTab}&page=${page + 1}">Trang sau</a></c:if>
</nav></c:if>
<c:if test="${sessionScope.user.role == 'DELIVERY_STAFF'}">
<script>
    // ThanhNC BA-35: stop a photo over 5 MB in the browser (MSG60); the server still checks every upload.
    document.querySelectorAll('form[data-evidence-max]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            var input = form.querySelector('input[name="evidence"]');
            var file = input && input.files && input.files[0];
            var old = form.querySelector('.evidence-size-error');
            if (old) { old.remove(); }
            if (file && file.size > Number(form.getAttribute('data-evidence-max'))) {
                e.preventDefault();
                var msg = document.createElement('div');
                msg.className = 'invalid-feedback d-block evidence-size-error';
                msg.setAttribute('role', 'alert');
                msg.textContent = form.getAttribute('data-evidence-message');
                input.insertAdjacentElement('afterend', msg);
                input.focus();
            }
        });
    });
</script>
</c:if>
<%@ include file="/WEB-INF/views/footer.jsp" %>
