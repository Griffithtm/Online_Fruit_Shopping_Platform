
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/header.jsp" %>

<div class="fulfillment-page">

    <div class="fulfillment-heading">
        <div>
            <span class="fulfillment-eyebrow">FRESHORA MANAGEMENT</span>
            <h1>Xử lý đơn hàng</h1>
            <p>Quản lý đơn hàng, giao nhận và quyết toán.</p>
            <label>Tìm đơn hàng<input id="orderSearch" class="form-control" placeholder="Mã đơn, sản phẩm hoặc cửa hàng"></label>
            <script>document.addEventListener("DOMContentLoaded", () => {document.getElementById("orderSearch").addEventListener("input", e => {document.querySelectorAll("section.panel").forEach(p => p.hidden = !p.textContent.toLowerCase().includes(e.target.value.toLowerCase())); }); });</script>
        </div>
        <div class="fulfillment-heading-icon">
            <i class="fas fa-box-open"></i>
        </div>
    </div>

    <%-- Địa chỉ lấy hàng của Seller --%>
    <c:if test="${sessionScope.user.role == 'SELLER'}">
        <div class="fulfillment-toolbar">
            <form method="post" class="pickup-form">
                <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                <input type="hidden" name="action" value="pickupAddress">

                <div class="pickup-icon">
                    <i class="fas fa-map-marker-alt"></i>
                </div>

                <div class="pickup-content">
                    <h3>Địa chỉ lấy hàng</h3>
                    <p>Chọn địa chỉ để nhân viên giao hàng đến lấy sản phẩm.</p>

                    <div class="pickup-controls">
                        <select name="address" required class="form-control">
                            <c:forEach items="${pickupAddresses}" var="a">
                                <option value="${a.id}">
                                    <c:out value="${a.addressLine}, ${a.city}"/>
                                </option>
                            </c:forEach>
                        </select>

                        <button type="submit" class="fulfillment-btn btn-primary-custom">
                            <i class="fas fa-save"></i>
                            Lưu địa chỉ
                        </button>
                    </div>

                    <small>
                        <i class="fas fa-info-circle"></i>
                        Thêm địa chỉ tại Tài khoản của tôi.
                    </small>
                </div>
            </form>

            <a href="${pageContext.request.contextPath}/seller/promotions"
               class="promotion-shortcut">
                <span class="shortcut-icon">
                    <i class="fas fa-tags"></i>
                </span>
                <span>
                    <strong>Khuyến mãi cửa hàng</strong>
                    <small>Quản lý chiến dịch khuyến mãi</small>
                </span>
                <i class="fas fa-chevron-right shortcut-arrow"></i>
            </a>
        </div>
    </c:if>

    <%-- Không có đơn hàng --%>
    <c:if test="${empty sellerOrders}">
        <div class="fulfillment-empty">
            <div class="empty-icon">
                <i class="fas fa-box-open"></i>
            </div>
            <h3>Chưa có đơn hàng</h3>
            <p>Chưa có đơn hàng nào để hiển thị.</p>
        </div>
    </c:if>

    <%-- Danh sách đơn hàng --%>
    <div class="fulfillment-list">

        <c:forEach items="${sellerOrders}" var="o"><section class="panel">
                <section class="fulfillment-card">

                    <%-- Header đơn hàng --%>
                    <div class="order-card-header">
                        <div class="order-heading-main">
                            <div class="order-icon">
                                <i class="fas fa-receipt"></i>
                            </div>

                            <div>
                                <span class="order-label">MÃ ĐƠN HÀNG #${o.id}</span>
                                <h2>
                                    <c:out value="${o.orderCode}"/>
                                </h2>
                                <span class="order-store">
                                    <i class="fas fa-store"></i>
                                    <c:out value="${o.storeName}"/>
                                </span>
                            </div>
                        </div>

                        <div class="order-status">
                            <span class="status-dot"></span>
                            <c:out value="${vi:label(o.status)}"/>
                        </div>
                    </div>

                    <%-- Thông tin thanh toán --%>
                    <div class="order-payment-strip">
                        <div>
                            <span class="info-label">Phương thức thanh toán</span>
                            <strong>
                                <i class="fas fa-credit-card"></i>
                                <c:out value="${vi:label(o.paymentMethod)}"/>
                            </strong>
                        </div>

                        <div>
                            <span class="info-label">Tiền cần thu khi giao</span>
                            <strong class="order-cod">
                                <fmt:formatNumber value="${o.codAmount}"/> đ
                            </strong>
                        </div>
                    </div>

                    <%-- Thông tin giao nhận --%>
                    <div class="order-section">
                        <h3 class="section-heading">
                            <i class="fas fa-truck"></i>
                            Thông tin giao nhận
                        </h3>

                        <div class="order-info-grid">
                            <div class="info-item">
                                <span class="info-label">Người nhận</span>
                                <strong>
                                    <i class="fas fa-user"></i>
                                    <c:out value="${o.recipientName}"/>
                                </strong>
                                <span>
                                    <i class="fas fa-phone"></i>
                                    <c:out value="${o.recipientPhone}"/>
                                </span>
                            </div>

                            <div class="info-item">
                                <span class="info-label">Địa chỉ giao hàng</span>
                                <strong>
                                    <i class="fas fa-map-marker-alt"></i>
                                    <c:out value="${o.deliveryAddress}"/>
                                </strong>
                            </div>

                            <div class="info-item">
                                <span class="info-label">Địa chỉ lấy hàng</span>
                                <strong>
                                    <i class="fas fa-store-alt"></i>
                                    <c:out value="${o.pickupAddress}, ${o.pickupCity}"/>
                                </strong>
                            </div>

                            <div class="info-item">
                                <span class="info-label">Hướng dẫn bảo quản lạnh</span>
                                <strong>
                                    <i class="fas fa-snowflake"></i>
                                    <c:out value="${o.coldChainNote}"/>
                                </strong>
                            </div>
                        </div>

                        <div class="order-reason">
                            <i class="fas fa-info-circle"></i>
                            <div>
                                <span class="info-label">Lý do từ chối/thất bại</span>
                                <p>
                                    <c:out value="${o.rejectionReason} ${o.failureReason}"/>
                                </p>
                            </div>
                        </div>
                    </div>

                    <%-- Sản phẩm và lô hàng --%>
                    <div class="order-section">
                        <h3 class="section-heading">
                            <i class="fas fa-boxes"></i>
                            Sản phẩm và thông tin lô hàng
                        </h3>

                        <div class="allocation-list">
                            <c:forEach items="${allocations}" var="a">
                                <c:if test="${a.sellerOrderId == o.id}">
                                    <div class="allocation-item">
                                        <div class="allocation-product">
                                            <div class="allocation-icon">
                                                <i class="fas fa-apple-alt"></i>
                                            </div>

                                            <div>
                                                <strong>
                                                    <c:out value="${a.title} - ${a.optionName}"/>
                                                </strong>
                                                <span>Số lượng: × ${a.quantity}</span>
                                            </div>
                                        </div>

                                        <div class="allocation-meta">
                                            <span>
                                                <i class="fas fa-barcode"></i>
                                                Lô <c:out value="${a.batchNo}"/>
                                            </span>
                                            <span>
                                                <i class="fas fa-calendar-alt"></i>
                                                Hạn sử dụng:
                                                <c:out value="${a.expiryDate}"/>
                                            </span>
                                            <span class="reservation-status">
                                                <c:out value="${vi:label(a.reservationStatus)}"/>
                                            </span>
                                        </div>
                                    </div>
                                </c:if>
                            </c:forEach>
                        </div>
                    </div>

                    <%-- Tổng tiền --%>
                    <div class="order-section order-financial">
                        <h3 class="section-heading">
                            <i class="fas fa-calculator"></i>
                            Tổng kết đơn hàng
                        </h3>

                        <div class="financial-row">
                            <span>Tổng tạm tính</span>
                            <strong>
                                <fmt:formatNumber value="${o.subtotal}"/> đ
                            </strong>
                        </div>

                        <div class="financial-row">
                            <span>Giảm giá</span>
                            <strong class="discount-value">
                                - <fmt:formatNumber value="${o.discountAmount + o.pointsDiscount}"/> đ
                            </strong>
                        </div>

                        <div class="financial-row">
                            <span>Phí giao hàng</span>
                            <strong>
                                <fmt:formatNumber value="${o.deliveryFee}"/> đ
                            </strong>
                        </div>
                    </div>

                    <%-- Các nút thao tác --%>
                    <c:if test="${o.orderStatus == 'CONFIRMED'}">
                        <div class="order-action-area">

                            <h3 class="section-heading">
                                <i class="fas fa-tasks"></i>
                                Thao tác xử lý
                            </h3>

                            <form method="post" class="order-action-form">
                                <input type="hidden" name="csrf"
                                       value="${sessionScope.csrf}">
                                <input type="hidden" name="id" value="${o.id}">

                                <%-- Seller --%>
                                <c:if test="${sessionScope.user.role == 'SELLER'}">

                                    <c:if test="${o.status == 'NEW'}">
                                        <button name="action" value="accept"
                                                class="fulfillment-btn btn-primary-custom">
                                            <i class="fas fa-check"></i>
                                            Nhận đơn
                                        </button>
                                    </c:if>

                                    <c:if test="${o.status == 'NEW' || o.status == 'ACCEPTED'}">
                                        <div class="action-input">
                                            <label>Lý do từ chối</label>
                                            <input class="form-control" name="detail"
                                                   maxlength="500"
                                                   placeholder="Nhập lý do từ chối đơn hàng...">
                                        </div>

                                        <button name="action" value="reject"
                                                class="fulfillment-btn btn-danger-custom">
                                            <i class="fas fa-times"></i>
                                            Từ chối đơn
                                        </button>
                                    </c:if>

                                    <c:if test="${o.status == 'ACCEPTED'}">
                                        <button name="action" value="ready"
                                                class="fulfillment-btn btn-success-custom">
                                            <i class="fas fa-box"></i>
                                            Đã đóng gói, sẵn sàng giao
                                        </button>
                                    </c:if>

                                </c:if>

                                <%-- Admin --%>
                                <c:if test="${sessionScope.user.role == 'ADMIN' && o.status == 'READY'}">

                                    <div class="action-input">
                                        <label>Nhân viên giao hàng</label>
                                        <select name="courier" class="form-control">
                                            <c:forEach items="${couriers}" var="c">
                                                <option value="${c.id}">
                                                    <c:out value="${c.fullName}"/>
                                                </option>
                                            </c:forEach>
                                        </select>
                                    </div>

                                    <div class="action-input">
                                        <label>Hướng dẫn bảo quản lạnh</label>
                                        <input name="detail" class="form-control"
                                               maxlength="255" required
                                               placeholder="Nhập hướng dẫn bảo quản...">
                                    </div>

                                    <button name="action" value="assign"
                                            class="fulfillment-btn btn-primary-custom">
                                        <i class="fas fa-user-check"></i>
                                        Phân công giao hàng
                                    </button>

                                </c:if>

                                <%-- Nhân viên giao hàng --%>
                                <c:if test="${sessionScope.user.role == 'DELIVERY_STAFF'}">

                                    <c:choose>
                                        <c:when test="${o.status == 'ASSIGNED'}">
                                            <button name="action" value="pickup"
                                                    class="fulfillment-btn btn-primary-custom">
                                                <i class="fas fa-box"></i>
                                                Xác nhận đã lấy hàng
                                            </button>
                                        </c:when>

                                        <c:when test="${o.status == 'PICKED_UP'}">
                                            <button name="action" value="transit"
                                                    class="fulfillment-btn btn-primary-custom">
                                                <i class="fas fa-shipping-fast"></i>
                                                Đang vận chuyển
                                            </button>
                                        </c:when>

                                        <c:when test="${o.status == 'IN_TRANSIT'}">
                                            <button name="action" value="arrive"
                                                    class="fulfillment-btn btn-primary-custom">
                                                <i class="fas fa-map-marker-alt"></i>
                                                Sắp giao đến
                                            </button>
                                        </c:when>
                                    </c:choose>

                                    <c:if test="${o.status == 'ARRIVING'}">
                                        <div class="action-input">
                                            <label>Đường dẫn bằng chứng giao hàng</label>
                                            <input name="detail" class="form-control"
                                                   maxlength="500"
                                                   placeholder="Nhập đường dẫn bằng chứng...">
                                        </div>

                                        <label class="collected-check">
                                            <input type="checkbox" name="collected">
                                            <span>Đã thu đủ tiền khi giao</span>
                                        </label>

                                        <button name="action" value="deliver"
                                                class="fulfillment-btn btn-success-custom">
                                            <i class="fas fa-check-circle"></i>
                                            Xác nhận đã giao
                                        </button>
                                    </c:if>

                                    <c:if test="${o.status == 'ASSIGNED' || o.status == 'PICKED_UP' || o.status == 'IN_TRANSIT'}">
                                        <div class="action-input">
                                            <label>Lý do giao thất bại</label>
                                            <input name="detail" class="form-control"
                                                   maxlength="500"
                                                   placeholder="Nhập lý do giao thất bại...">
                                        </div>
                                    </c:if>

                                    <c:if test="${o.status == 'ASSIGNED' || o.status == 'PICKED_UP' || o.status == 'IN_TRANSIT' || o.status == 'ARRIVING'}">
                                        <button name="action" value="fail"
                                                class="fulfillment-btn btn-danger-custom">
                                            <i class="fas fa-exclamation-circle"></i>
                                            Báo giao hàng thất bại
                                        </button>
                                    </c:if>

                                </c:if>

                            </form>
                        </div>
                    </c:if>

                    <%-- Quyết toán --%>
                    <div class="settlement-area">
                        <div class="settlement-heading">
                            <i class="fas fa-file-invoice-dollar"></i>
                            <strong>Thông tin quyết toán</strong>
                        </div>

                        <div class="settlement-grid">
                            <div>
                                <span class="info-label">Trạng thái</span>
                                <strong>
                                    <c:out value="${vi:label(o.settlementStatus)}"/>
                                </strong>
                            </div>

                            <div>
                                <span class="info-label">Hoa hồng</span>
                                <strong>
                                    <fmt:formatNumber value="${o.commission}"/> đ
                                </strong>
                            </div>

                            <div>
                                <span class="info-label">Thực nhận của người bán</span>
                                <strong class="net-payable">
                                    <fmt:formatNumber value="${o.netPayable}"/> đ
                                </strong>
                            </div>
                        </div>

                        <c:if test="${sessionScope.user.role == 'ADMIN' && o.status == 'DELIVERED' && o.settlementStatus == 'UNSETTLED'}">
                            <form method="post" class="settlement-form">
                                <input type="hidden" name="csrf"
                                       value="${sessionScope.csrf}">
                                <input type="hidden" name="id" value="${o.id}">

                                <button class="fulfillment-btn btn-success-custom"
                                        name="action" value="settle">
                                    <i class="fas fa-check-double"></i>
                                    Ghi nhận quyết toán đủ điều kiện
                                </button>
                            </form>
                        </c:if>
                    </div>

                </section>
            </section></c:forEach>

        </div>
    </div>

<%@ include file="/WEB-INF/views/footer.jsp" %>
