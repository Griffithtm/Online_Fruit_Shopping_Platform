<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <h1><c:out value="${empty shop ? 'Khám phá sản phẩm' : shop.storeName}"/></h1>
        <p>
            <c:out value="${shop.description}" />
        </p>
        <form class="filters" method="get">
            <c:if test="${not empty shop}">
                <input type="hidden" name="id" value="${shop.id}">
            </c:if>
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
        <%@ include file="/WEB-INF/views/cards.jsp" %>
            <%@ include file="/WEB-INF/views/footer.jsp" %>