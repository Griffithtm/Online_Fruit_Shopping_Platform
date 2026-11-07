<%@ page pageEncoding="UTF-8" %><%@ include file="/WEB-INF/views/header.jsp" %>
<h1>Thanh toán</h1><p><a href="${pageContext.request.contextPath}/cart">Quay lại giỏ hàng</a></p>
<form method="get" class="panel">
    <label>Địa chỉ giao hàng<select class="form-control" name="address" required><c:forEach items="${addresses}" var="a"><option value="${a.id}" ${addressId == a.id ? 'selected' : ''}><c:out value="${a.recipientName} - ${a.addressLine}, ${a.city}"/></option></c:forEach></select></label>
        <label>Mã giảm giá<input name="voucher" class="form-control" maxlength="30"></label><label>Điểm thưởng<input type="number" name="points" value="0" min="0" class="form-control"></label>
        <label>Phương thức thanh toán<select name="method" class="form-control"><option value="COD">Thanh toán khi nhận hàng</option><option value="CARD">Thẻ qua VNPay</option><option value="E_WALLET">Mã QR VNPay / ví điện tử</option></select></label><button class="button">Kiểm tra đơn hàng</button></form>
<c:if test="${empty addresses}"><p>Thêm địa chỉ giao hàng trong <a href="${pageContext.request.contextPath}/account">Tài khoản của tôi</a> trước khi tiếp tục.</p></c:if>
<c:if test="${not empty quote}">
    <section class="checkout-summary">

        <!-- HEADER -->
        <div class="checkout-heading">
            <div>
                <span class="checkout-eyebrow">FRESHORA CHECKOUT</span>
                <h2>Tóm tắt đơn hàng</h2>
                <p>Vui lòng kiểm tra thông tin trước khi đặt hàng</p>
            </div>
            <div class="checkout-heading-icon">
                <i class="fa-solid fa-basket-shopping"></i>
            </div>
        </div>

        <!-- ĐỊA CHỈ NHẬN HÀNG -->
        <div class="checkout-section">
            <div class="checkout-section-title">
                <i class="fa-solid fa-location-dot"></i>
                <h3>Địa chỉ nhận hàng</h3>
            </div>

            <div class="checkout-address">
                <div class="address-recipient">
                    <strong>
                        <c:out value="${quote.address.recipientName}"/>
                    </strong>
                    <span class="address-divider"></span>
                    <span>
                        <c:out value="${quote.address.phone}"/>
                    </span>
                </div>

                <p>
                    <c:out value="${quote.address.addressLine},
                    ${quote.address.ward},
                    ${quote.address.district},
                    ${quote.address.city}"/>
                </p>
            </div>
        </div>

        <!-- DANH SÁCH SẢN PHẨM -->
        <div class="checkout-section">
            <div class="checkout-section-title">
                <i class="fa-solid fa-box-open"></i>
                <h3>Sản phẩm trong đơn hàng</h3>
            </div>

            <div class="checkout-products">
                <c:forEach items="${quote.lines}" var="l">
                    <div class="checkout-product">
                        <div class="product-store">
                            <i class="fa-solid fa-store"></i>
                            <strong><c:out value="${l.storeName}"/></strong>
                        </div>

                        <div class="product-info">
                            <div class="product-name">
                                <c:out value="${l.title}"/>
                            </div>

                            <div class="product-quantity">
                                x ${l.quantity}
                            </div>

                            <div class="product-price">
                                <fmt:formatNumber
                                    value="${l.price * l.quantity}"/> đ
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>

        <!-- TỔNG TIỀN THEO CỬA HÀNG -->
        <div class="checkout-section">
            <div class="checkout-section-title">
                <i class="fa-solid fa-receipt"></i>
                <h3>Tổng tiền theo cửa hàng</h3>
            </div>

            <div class="seller-totals">
                <c:forEach items="${quote.groups}" var="g">
                    <div class="seller-total-card">
                        <div class="seller-total-name">
                            <i class="fa-solid fa-shop"></i>
                            <strong>
                                <c:out value="${g.value.storeName}"/>
                            </strong>
                        </div>

                        <div class="seller-total-details">
                            <div>
                                <span>Tiền hàng</span>
                                <strong>
                                    <fmt:formatNumber
                                        value="${g.value.subtotal}"/> đ
                                </strong>
                            </div>

                            <div>
                                <span>Giảm giá</span>
                                <strong class="discount-text">
                                    -<fmt:formatNumber
                                        value="${g.value.discount + g.value.pointsDiscount}"/> đ
                                </strong>
                            </div>

                            <div>
                                <span>Phí giao hàng</span>
                                <strong>
                                    <fmt:formatNumber
                                        value="${g.value.fee}"/> đ
                                </strong>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>

        <!-- CHI TIẾT THANH TOÁN -->
        <div class="checkout-section payment-breakdown">
            <div class="checkout-section-title">
                <i class="fa-solid fa-calculator"></i>
                <h3>Chi tiết thanh toán</h3>
            </div>

            <div class="payment-row">
                <span>Tiền hàng</span>
                <strong>
                    <fmt:formatNumber value="${quote.subtotal}"/> đ
                </strong>
            </div>

            <div class="payment-row">
                <span>Phí giao hàng</span>
                <strong>
                    <fmt:formatNumber value="${quote.fee}"/> đ
                </strong>
            </div>

            <div class="payment-row">
                <span>Giảm giá khuyến mãi</span>
                <strong class="discount-text">
                    -<fmt:formatNumber value="${quote.discount}"/> đ
                </strong>
            </div>

            <div class="payment-row">
                <span>Giảm giá từ điểm thưởng</span>
                <strong class="discount-text">
                    -<fmt:formatNumber value="${quote.pointsDiscount}"/> đ
                </strong>
            </div>
        </div>

        <!-- GIAO HÀNG VÀ THANH TOÁN -->
        <div class="checkout-extra">
            <div class="checkout-extra-item">
                <div class="extra-icon">
                    <i class="fa-solid fa-truck-fast"></i>
                </div>
                <div>
                    <span class="extra-label">Thời gian giao hàng dự kiến</span>
                    <strong>
                        Dự kiến giao trong ${quote.etaHours} giờ sau khi xác nhận.
                    </strong>
                </div>
            </div>

            <div class="checkout-extra-item">
                <div class="extra-icon">
                    <i class="fa-solid fa-wallet"></i>
                </div>
                <div>
                    <span class="extra-label">Phương thức thanh toán</span>
                    <strong>
                        <c:out value="${vi:label(quote.method)}"/>
                    </strong>
                </div>
            </div>
        </div>

        <!-- TỔNG CỘNG -->
        <div class="checkout-final">
            <div class="final-label">
                <span>Tổng thanh toán</span>
                <small>Đã bao gồm phí giao hàng và các khoản giảm giá</small>
            </div>

            <div class="final-amount">
                <fmt:formatNumber value="${quote.total}"/> đ
            </div>
        </div>

        <!-- NÚT ĐẶT HÀNG -->
        <div class="checkout-submit">
            <div class="submit-note">
                <i class="fa-solid fa-shield-halved"></i>
                Kiểm tra kỹ thông tin trước khi đặt đơn hàng.
            </div>

            <form method="post">
                <input type="hidden" name="csrf"
                       value="${sessionScope.csrf}">
                <input type="hidden" name="key"
                       value="${checkoutKey}">

                <button type="submit" class="checkout-place-button">
                    <i class="fa-solid fa-check"></i>
                    Đặt hàng
                </button>
            </form>
        </div>

    </section>
</c:if>
        <%@ include file="/WEB-INF/views/footer.jsp" %>
