<%@ page pageEncoding="UTF-8" %><%@ include file="/WEB-INF/views/header.jsp" %>
<h1>Thanh toán</h1><p><a href="${pageContext.request.contextPath}/cart">Quay lại giỏ hàng</a></p>
<form method="get" class="panel">
    <label>Địa chỉ giao hàng<select class="form-control" name="address" required><c:forEach items="${addresses}" var="a"><option value="${a.id}" ${addressId == a.id ? 'selected' : ''}><c:out value="${a.recipientName} - ${a.addressLine}, ${a.city}"/></option></c:forEach></select></label>
        <label>Mã giảm giá<input name="voucher" class="form-control" maxlength="30" value="${fn:escapeXml(form.voucher)}"></label>
        <label>Điểm thưởng<input type="number" name="points" value="${empty form.points ? 0 : fn:escapeXml(form.points)}" min="0" step="1" class="form-control" aria-describedby="points-help"></label>
        <c:if test="${not empty quote}"><small id="points-help" class="form-text text-muted">Bạn có thể dùng tối đa ${quote.maxPoints} điểm cho đơn hàng này (số dư: ${quote.usablePoints} điểm).</small></c:if>
        <label>Phương thức thanh toán<select name="method" class="form-control">
            <option value="COD" ${form.method == 'COD' ? 'selected' : ''}>Thanh toán khi nhận hàng</option>
            <option value="CARD" ${form.method == 'CARD' ? 'selected' : ''}>Thẻ qua VNPay thử nghiệm</option>
            <option value="E_WALLET" ${form.method == 'E_WALLET' ? 'selected' : ''}>Mã QR VNPay / ví điện tử được hỗ trợ</option></select></label><button class="button">Kiểm tra đơn hàng</button></form>
<c:if test="${empty addresses}"><p>Thêm địa chỉ giao hàng trong <a href="${pageContext.request.contextPath}/account">Tài khoản của tôi</a> trước khi tiếp tục.</p></c:if>
<c:if test="${not empty quote}"><section class="panel"><h2>Tóm tắt đơn hàng</h2><p><c:out value="${quote.address.recipientName} - ${quote.address.phone}"/></p><p><c:out value="${quote.address.addressLine}, ${quote.address.ward}, ${quote.address.district}, ${quote.address.city}"/></p>
        <c:forEach items="${quote.lines}" var="l"><p><c:out value="${l.storeName} - ${l.title}"/> x ${l.quantity}: <fmt:formatNumber value="${l.price * l.quantity}"/> đ</p></c:forEach>
        <h3>Tổng tiền theo cửa hàng</h3><c:forEach items="${quote.groups}" var="g"><p><c:out value="${g.value.storeName}"/>: tiền hàng <fmt:formatNumber value="${g.value.subtotal}"/> - giảm giá <fmt:formatNumber value="${g.value.discount + g.value.pointsDiscount}"/> + phí giao hàng <fmt:formatNumber value="${g.value.fee}"/> đ</p></c:forEach>
        <p>Tiền hàng: <fmt:formatNumber value="${quote.subtotal}"/> đ</p><p>Phí giao hàng: <fmt:formatNumber value="${quote.fee}"/> đ (${fn:length(quote.groups)} cửa hàng × <fmt:formatNumber value="${quote.areaFee}"/> đ, mỗi cửa hàng giao riêng)</p><p>Giảm giá khuyến mãi: <fmt:formatNumber value="${quote.discount}"/> đ</p><p>Giảm giá từ điểm thưởng: <fmt:formatNumber value="${quote.pointsDiscount}"/> đ</p><p>Dự kiến giao trong ${quote.etaHours} giờ sau khi xác nhận.</p>
        <h3>Tổng cộng: <fmt:formatNumber value="${quote.total}"/> đ</h3><p>Phương thức thanh toán: <c:out value="${vi:label(quote.method)}"/></p>
        <form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="key" value="${checkoutKey}"><button class="button">Đặt hàng</button></form></section></c:if>
        <%@ include file="/WEB-INF/views/footer.jsp" %>
