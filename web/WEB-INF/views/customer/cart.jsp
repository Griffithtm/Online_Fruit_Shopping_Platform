<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <h1>Giỏ hàng của bạn</h1>
        <p>Thêm vào giỏ chưa giữ tồn kho. Giá và số lượng sẽ được kiểm tra lại khi đặt hàng.</p>
        <c:if test="${empty lines}">
            <div class="empty">Giỏ hàng đang trống. <a href="${pageContext.request.contextPath}/products">Khám phá sản phẩm</a>
            </div>
        </c:if>
        <c:forEach items="${totals}" var="shop">
            <section class="panel">
                <h2><c:out value="${shop.key}"/></h2>
                <c:forEach items="${lines}" var="l">
                    <c:if test="${l.storeName == shop.key}">
                        <div class="cartline">
                            <div>
                                <h3><c:out value="${l.title}"/></h3>
                                <c:if test="${l.purchasable == 0}"><p role="alert">Sản phẩm này đã ngừng bán. Vui lòng xóa khỏi giỏ hàng.</p></c:if>
                                <c:if test="${l.quantity > l.availableQty}"><p role="alert">Tồn kho đã thay đổi. Vui lòng giảm số lượng trước khi thanh toán.</p></c:if>
                                <c:if test="${l.savedPrice != l.unitPrice}"><p role="alert">Giá đã thay đổi từ <fmt:formatNumber value="${l.savedPrice}"/> đ. Nhấn Cập nhật để chấp nhận giá hiện tại.</p></c:if>
                                <c:out value="${l.optionName}" /> ·
                                <fmt:formatNumber value="${l.unitPrice}" /> đ · Còn ${l.availableQty}</div>
                            <form method="post" action="">
                                <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                                <input type="hidden" name="action" value="update">
                                <input type="hidden" name="id" value="${l.id}">
                                <label>Số lượng
                                    <input class="form-control" name="quantity" type="number" value="${l.quantity}" required>
                                </label>
                                <button class="button">Cập nhật</button>
                            </form>
                            <form method="post" action="">
                                <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                                <input type="hidden" name="action" value="remove">
                                <input type="hidden" name="id" value="${l.id}">
                                <button class="button">Xóa</button>
                            </form><strong><fmt:formatNumber value="${l.lineTotal}"/> đ</strong>
                        </div>
                    </c:if>
                </c:forEach>
                <p>Tạm tính cửa hàng: <strong><fmt:formatNumber value="${shop.value}"/> đ</strong>
                </p>
            </section>
        </c:forEach>
        <h2>Tổng tạm tính: <fmt:formatNumber value="${total}"/> đ</h2>
        <c:if test="${not empty lines}"><a class="button" href="${pageContext.request.contextPath}/checkout">Tiến hành thanh toán</a></c:if><p><a href="${pageContext.request.contextPath}/orders">Đơn hàng của tôi</a></p>
        <%@ include file="/WEB-INF/views/footer.jsp" %>