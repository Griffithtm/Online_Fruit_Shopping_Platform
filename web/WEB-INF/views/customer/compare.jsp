<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <h1>So sánh lựa chọn từ các cửa hàng</h1>
        <p>So sánh loại trái cây, xuất xứ và quy cách đóng gói trước khi chọn mua. Giá mỗi kg chỉ được tính khi sản phẩm có khối lượng rõ ràng (ví dụ: 500 g hoặc 1 kg).</p>
        <form class="filters" method="get">
            <label>Tìm sản phẩm
                <input class="form-control" name="q" value="${fn:escapeXml(param.q)}" placeholder="Cam, xoài, táo...">
            </label>
            <label>Danh mục
                <select class="form-select" name="category">
                    <option value="">Tất cả</option>
                    <c:forEach items="${categories}" var="x">
                        <option value="${x.id}" ${param.category==x .id ? 'selected' : ''}>
                            <c:out value="${x.name}" />
                        </option>
                    </c:forEach>
                </select>
            </label>
            <label>Tình trạng hàng <select class="form-select" name="availability">
<option value="in" ${empty param.availability or param.availability == 'in' ? 'selected' : ''}>Còn hàng</option>
<option value="out" ${param.availability == 'out' ? 'selected' : ''}>Hết hàng</option>
<option value="all" ${param.availability == 'all' ? 'selected' : ''}>Tất cả</option></select></label>
<c:if test="${empty shop}"><label>Người bán <select class="form-select" name="store"><option value="">Tất cả cửa hàng</option><c:forEach items="${shops}" var="seller"><option value="${seller.id}" ${param.store == seller.id ? 'selected' : ''}><c:out value="${seller.storeName}"/></option></c:forEach></select></label></c:if>
<label>Giá từ
                <input class="form-control" type="number" min="0" name="min" value="${fn:escapeXml(param.min)}">
            </label>
            <label>Đến
                <input class="form-control" type="number" min="0" name="max" value="${fn:escapeXml(param.max)}">
            </label>
            <label>Đánh giá tối thiểu
                <select class="form-select" name="rating">
                    <option value="">Tất cả</option>
                    <option value="4" ${param.rating == '4' ? 'selected' : ''}>4 sao</option>
                    <option value="3" ${param.rating == '3' ? 'selected' : ''}>3 sao</option>
                </select>
            </label>
            <label>Sắp xếp
                <select class="form-select" name="sort">
                    <option value="unitPrice" ${param.sort == 'unitPrice' ? 'selected' : ''}>Giá mỗi kg: tăng dần</option><option value="new" ${param.sort == 'new' ? 'selected' : ''}>Mới nhất</option>
                    <option value="priceAsc" ${param.sort == 'priceAsc' ? 'selected' : ''}>Giá tăng</option>
                    <option value="priceDesc" ${param.sort == 'priceDesc' ? 'selected' : ''}>Giá giảm</option>
                    <option value="rating" ${param.sort == 'rating' ? 'selected' : ''}>Đánh giá</option>
                    <option value="popular" ${param.sort == 'popular' ? 'selected' : ''}>Phổ biến</option>
                </select>
            </label>
            <label>
                <input type="checkbox" name="promo" value="1" ${param.promo == '1' ? 'checked' : ''}> Có khuyến mãi</label>
            <button class="button">Tìm kiếm</button>
        </form>
        <div class="table-responsive">
            <table class="table">
                <tr>
                    <th>Sản phẩm</th>
                    <th>Cửa hàng</th>
                    <th>Quy cách / đơn vị</th>
                    <th>Giá</th>
                    <th>Đánh giá cửa hàng</th>
                    <th>Tồn</th><th>Giá mỗi kg</th><th>Xuất xứ</th><th>Điều kiện giao hàng</th>
                </tr>
                <c:forEach items="${products}" var="p">
                    <tr>
                        <td>
                            <a href="${pageContext.request.contextPath}/product?id=${p.listingId}">
                                <c:out value="${p.title}" />
                            </a>
                        </td>
                        <td>
                            <c:out value="${p.storeName}" />
                        </td>
                        <td>
                            <c:out value="${p.optionName}" /> /
                            <c:out value="${p.unit}" />
                        </td>
                        <td>
                            <fmt:formatNumber value="${p.price}" /> đ</td>
                        <td>${p.sellerRating}/5</td>
                        <td>${p.availableQty}</td><td><c:choose><c:when test="${not empty p.unitPriceKg}"><fmt:formatNumber value="${p.unitPriceKg}"/> đ/kg</c:when><c:otherwise>Chưa xác định khối lượng</c:otherwise></c:choose></td><td><c:out value="${p.origin}"/></td><td>Giao hàng qua nền tảng; xem khu vực phục vụ bên dưới.</td>
                    </tr>
                </c:forEach>
            </table>
        </div>
        <section class="panel"><h2>Khu vực giao hàng</h2><p>Điều kiện giao hàng này áp dụng cho tất cả cửa hàng. Phí và thời gian giao dự kiến được xác nhận theo địa chỉ của bạn khi thanh toán.</p>
<c:forEach items="${deliveryAreas}" var="area"><p><c:out value="${area.name}"/>: <c:choose><c:when test="${not empty area.deliveryFee}"><fmt:formatNumber value="${area.deliveryFee}"/> đ</c:when><c:otherwise>Phí được xác nhận khi thanh toán</c:otherwise></c:choose></p></c:forEach>
<c:if test="${empty deliveryAreas}"><p>Chưa thiết lập khu vực giao hàng.</p></c:if></section>
<%@ include file="/WEB-INF/views/footer.jsp" %>