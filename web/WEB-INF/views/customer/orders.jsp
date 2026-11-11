
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/header.jsp" %>

<div class="my-orders-page">

    <%-- Tiêu đề --%>
    <div class="my-orders-heading">
        <div>
            <span class="orders-eyebrow">FRESHORA · CUSTOMER CENTER</span>
            <h1>Đơn hàng của tôi</h1>
            <p>Theo dõi đơn hàng, thanh toán và trạng thái giao hàng của bạn.</p>
        </div>

        <div class="orders-heading-icon">
            <i class="fas fa-shopping-bag"></i>
        </div>
    </div>

    <%-- Liên kết chức năng --%>

    <%-- Tìm kiếm --%>
    <div class="orders-search">
        <i class="fas fa-search"></i>
        <input id="orderSearch"
               class="form-control"
               type="search"
               placeholder="Tìm theo mã đơn, sản phẩm hoặc cửa hàng...">
    </div>

    <%-- Không có đơn hàng --%>
    <c:if test="${empty orders}">
        <div class="orders-empty">
            <div class="orders-empty-icon">
                <i class="fas fa-box-open"></i>
            </div>
            <h3>Bạn chưa có đơn hàng nào</h3>
            <p>Những đơn hàng bạn đặt sẽ được hiển thị tại đây.</p>
            <a href="${pageContext.request.contextPath}/products"
               class="orders-shop-button">
                <i class="fas fa-shopping-bag"></i>
                Khám phá sản phẩm
            </a>
        </div>
    </c:if>

    <%-- Danh sách đơn hàng --%>
    <div class="my-orders-list" id="orderList">

        <c:forEach items="${orders}" var="o">
            <section class="my-order-card">

                <%-- Header đơn hàng --%>
                <div class="my-order-header">
                    <div class="my-order-title">
                        <div class="my-order-icon">
                            <i class="fas fa-receipt"></i>
                        </div>

                        <div>
                            <span class="my-order-label">MÃ ĐƠN HÀNG #${o.id}</span>
                            <h2><c:out value="${o.orderCode}"/></h2>
                        </div>
                    </div>

                    <div class="my-order-status">
                        <span class="status-dot"></span>
                        <c:out value="${vi:label(o.status)}"/>
                    </div>
                </div>

                <%-- Trạng thái thanh toán --%>
                <div class="my-order-payment">
                    <div>
                        <span class="my-order-caption">Phương thức thanh toán</span>
                        <strong>
                            <i class="fas fa-credit-card"></i>
                            <c:out value="${vi:label(o.paymentMethod)}"/>
                        </strong>
                    </div>

                    <div>
                        <span class="my-order-caption">Trạng thái thanh toán</span>
                        <strong>
                            <i class="fas fa-wallet"></i>
                            <c:out value="${vi:label(o.paymentStatus)}"/>
                        </strong>
                    </div>
                </div>

                <%-- Địa chỉ nhận hàng --%>
                <div class="my-order-section">
                    <h3 class="my-order-section-title">
                        <i class="fas fa-map-marker-alt"></i>
                        Thông tin nhận hàng
                    </h3>

                    <div class="recipient-info">
                        <i class="fas fa-user"></i>
                        <strong><c:out value="${o.recipientName}"/></strong>
                    </div>

                    <div class="recipient-address">
                        <i class="fas fa-map-marker-alt"></i>
                        <span><c:out value="${o.deliveryAddress}"/></span>
                    </div>
                </div>

                <%-- Các đơn hàng cửa hàng và sản phẩm --%>
                <div class="my-order-section">
                    <h3 class="my-order-section-title">
                        <i class="fas fa-store"></i>
                        Chi tiết sản phẩm và cửa hàng
                    </h3>

                    <div class="customer-order-items">
                        <c:forEach items="${items}" var="i">
                            <c:if test="${i.orderId == o.id}">

                                <div class="customer-order-item">

                                    <div class="customer-product-heading">
                                        <div class="customer-product-icon">
                                            <i class="fas fa-apple-alt"></i>
                                        </div>

                                        <div class="customer-product-name">
                                            <strong>
                                                <c:out value="${i.title}"/>
                                            </strong>
                                            <span>
                                                <i class="fas fa-store"></i>
                                                <c:out value="${i.storeName}"/>
                                            </span>
                                        </div>
                                    </div>

                                    <div class="customer-product-details">
                                        <span>
                                            Số lượng:
                                            <strong>× ${i.quantity}</strong>
                                        </span>

                                        <strong class="customer-product-price">
                                            <fmt:formatNumber
                                                value="${i.unitPrice * i.quantity}"/> đ
                                        </strong>
                                    </div>

                                    <div class="customer-seller-status">
                                        <span>
                                            Trạng thái:
                                            <strong>
                                                <c:out value="${vi:label(i.sellerStatus)}"/>
                                            </strong>
                                        </span>

                                        <span>
                                            Đơn cửa hàng #${i.sellerOrderId}
                                        </span>
                                    </div>

                                    <c:if test="${not empty i.rejectionReason}">
                                        <div class="customer-rejection">
                                            <i class="fas fa-info-circle"></i>
                                            <span>
                                                <c:out value="${i.rejectionReason}"/>
                                            </span>
                                        </div>
                                    </c:if>

                                    <c:if test="${i.sellerStatus == 'NEW' || i.sellerStatus == 'ACCEPTED'}">
                                        <form method="post"
                                              class="customer-cancel-form">
                                            <input type="hidden" name="csrf"
                                                   value="${sessionScope.csrf}">
                                            <input type="hidden" name="action"
                                                   value="cancel">
                                            <input type="hidden" name="id"
                                                   value="${i.sellerOrderId}">

                                            <button class="customer-cancel-button"
                                                    type="submit">
                                                <i class="fas fa-times-circle"></i>
                                                Hủy đơn của cửa hàng này
                                            </button>
                                        </form>
                                    </c:if>

                                </div>

                            </c:if>
                        </c:forEach>
                    </div>
                </div>

                <%-- Tổng kết tiền --%>
                <div class="my-order-section my-order-financial">
                    <h3 class="my-order-section-title">
                        <i class="fas fa-calculator"></i>
                        Tổng kết đơn hàng
                    </h3>

                    <div class="my-order-money-row">
                        <span>Phí giao hàng</span>
                        <strong>
                            <fmt:formatNumber value="${o.deliveryFee}"/> đ
                        </strong>
                    </div>

                    <div class="my-order-money-row">
                        <span>Số tiền phải trả sau khi hủy đơn cửa hàng</span>
                        <strong class="current-payable">
                            <fmt:formatNumber value="${o.currentPayable}"/> đ
                        </strong>
                    </div>

                    <div class="my-order-original-total">
                        <span>Tổng tiền ban đầu</span>
                        <strong>
                            <fmt:formatNumber value="${o.payableAmount}"/> đ
                        </strong>
                    </div>
                </div>

                <%-- Thanh toán VNPay --%>
                <c:if test="${o.status == 'AWAITING_PAYMENT'}">
                    <div class="my-order-section my-order-vnpay">
                        <div class="vnpay-info">
                            <div class="vnpay-icon">
                                <i class="fas fa-credit-card"></i>
                            </div>

                            <div>
                                <strong>Đơn hàng đang chờ thanh toán</strong>
                                <p>
                                    Bạn có thể tiếp tục thanh toán hoặc thử lại với VNPay.
                                </p>
                            </div>
                        </div>

                        <form method="post"
                              action="${pageContext.request.contextPath}/payment/start">
                            <input type="hidden" name="csrf"
                                   value="${sessionScope.csrf}">
                            <input type="hidden" name="id" value="${o.id}">

                            <button class="customer-vnpay-button" type="submit">
                                <i class="fas fa-lock"></i>
                                Thanh toán / thử lại với VNPay
                            </button>
                        </form>

                        <div class="payment-expiry">
                            <i class="fas fa-clock"></i>
                            Hạn thanh toán:
                            <strong><c:out value="${o.paymentExpiresAt}"/></strong>
                            giờ quốc tế
                        </div>
                    </div>
                </c:if>

                <%-- Lịch sử thanh toán --%>
                <details class="my-order-history">
                    <summary>
                        <span>
                            <i class="fas fa-history"></i>
                            Lịch sử thử thanh toán
                        </span>
                        <i class="fas fa-chevron-down history-arrow"></i>
                    </summary>

                    <div class="payment-history-list">
                        <c:forEach items="${payments}" var="p">
                            <c:if test="${p.orderId == o.id}">
                                <div class="payment-history-item">
                                    <div class="payment-history-id">
                                        <strong>#${p.id}</strong>
                                        <span>
                                            <c:out value="${vi:label(p.method)}"/>
                                        </span>
                                    </div>

                                    <div class="payment-history-details">
                                        <strong>
                                            <fmt:formatNumber value="${p.amount}"/> đ
                                        </strong>
                                        <span>
                                            <c:out value="${vi:label(p.status)}"/>
                                        </span>
                                        <small>
                                            <fmt:formatDate
                                                value="${p.createdAt}"
                                                pattern="dd/MM/yyyy HH:mm"/>
                                            giờ quốc tế
                                        </small>
                                    </div>
                                </div>
                            </c:if>
                        </c:forEach>
                    </div>
                </details>

                <%-- Thời gian giao hàng --%>
                <div class="my-order-footer">
                    <i class="fas fa-truck"></i>
                    <span>Dự kiến giao hàng</span>
                    <strong>
                        <fmt:formatDate
                            value="${o.estimatedDeliveryAt}"
                            pattern="dd/MM/yyyy HH:mm"/>
                        giờ quốc tế
                    </strong>
                </div>

            </section>
        </c:forEach>

    </div>

    <div id="orderNoResult" class="orders-no-result" hidden>
        <i class="fas fa-search"></i>
        <strong>Không tìm thấy đơn hàng phù hợp</strong>
        <span>Thử tìm bằng mã đơn hàng, tên sản phẩm hoặc cửa hàng khác.</span>
    </div>

</div>

<script>
    document.addEventListener("DOMContentLoaded", () => {
        const search = document.getElementById("orderSearch");
        const orderList = document.getElementById("orderList");
        const noResult = document.getElementById("orderNoResult");

        if (!search || !orderList)
            return;

        const cards = orderList.querySelectorAll(".my-order-card");

        search.addEventListener("input", () => {
            const keyword = search.value.trim().toLowerCase();
            let visibleCount = 0;

            cards.forEach(card => {
                const matched = card.textContent.toLowerCase().includes(keyword);
                card.hidden = !matched;

                if (matched)
                    visibleCount++;
            });

            if (noResult) {
                noResult.hidden = visibleCount > 0 || keyword === "";
            }
        });
    });
</script>

<%@ include file="/WEB-INF/views/footer.jsp" %>
