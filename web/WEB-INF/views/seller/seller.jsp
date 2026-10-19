<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/header.jsp" %>
<h1>Quản lý cửa hàng</h1>
<p>Sản phẩm được đăng bán sau khi vượt qua kiểm tra tự động.
    Quản trị viên có thể hậu kiểm và đình chỉ sản phẩm vi phạm.</p>
<div class="two-column">
    <section class="panel">
        <h2>Tạo / sửa tin đăng</h2>
        <form method="post" action="">
            <input type="hidden" name="csrf" value="${sessionScope.csrf}">
            <input type="hidden" name="action" value="listing">
            <label>Mã tin đăng (được chọn tự động)
                <input class="form-control" name="id" type="number" readonly value="${editListing.id}">
            </label>
            <label>Tên trái cây
                <input class="form-control" name="title" maxlength="200" autocomplete="off" data-suggestions="${pageContext.request.contextPath}/seller/suggestions" data-product-url="${pageContext.request.contextPath}/product" type="text" value="${fn:escapeXml(editListing.title)}" required>
            </label>
            <label>Mô tả
                <input class="form-control" name="description" type="text" value="${fn:escapeXml(editListing.description)}" required>
            </label>
            <label>Danh mục
                <select class="form-select" name="category" required>
                    <c:forEach items="${categories}" var="x">
                        <option value="${x.id}" ${editListing.categoryId == x.id ? 'selected' : ''}>
                            <c:out value="${x.name}" /> (#${x.id})</option>
                        </c:forEach>
                </select>
            </label>
            <label>Xuất xứ
                <input class="form-control" name="origin" type="text" value="${fn:escapeXml(editListing.origin)}" required>
            </label>
            <label>Thông tin thu hoạch
                <input class="form-control" name="harvest" type="text" value="${fn:escapeXml(editListing.harvestInfo)}">
            </label>
            <button class="button">Lưu bản nháp</button> <a href="${pageContext.request.contextPath}/seller">Tin đăng mới</a>
        </form>
    </section>
    <section class="panel">
        <h2>Tạo / sửa biến thể và giá</h2>
        <form method="post" action="">
            <input type="hidden" name="csrf" value="${sessionScope.csrf}">
            <input type="hidden" name="action" value="variant">
            <label>Mã biến thể (được chọn tự động)
                <input class="form-control" name="id" type="number" readonly value="${editVariant.id}">
            </label>
            <label>Tin đăng
                <select class="form-select" name="listing" required>
                    <c:forEach items="${listings}" var="x">
                        <option value="${x.id}" ${editVariant.listingId == x.id ? 'selected' : ''}>
                            <c:out value="${x.title}" /> (#${x.id})</option>
                        </c:forEach>
                </select>
            </label>
            <label>Quy cách (gói 1kg, hộp 500g...)
                <input class="form-control" name="option" type="text" value="${fn:escapeXml(editVariant.optionName)}" required>
            </label>
            <label>Đơn vị bán
                <input class="form-control" name="unit" type="text" value="${fn:escapeXml(editVariant.unit)}" required>
            </label>
            <label>Giá bán (đ)
                <input class="form-control" name="price" type="number" value="${fn:escapeXml(editVariant.price)}" required>
            </label>
            <button class="button">Lưu biến thể</button> <a href="${pageContext.request.contextPath}/seller">Biến thể mới</a>
        </form>
    </section>
</div>
<h2>Tin đăng</h2>
<div class="table-responsive">
    <table class="table">
        <tr>
            <th>Mã / Tên</th>
            <th>Trạng thái</th>
            <th>Lý do</th>
            <th>Thao tác</th>
        </tr>
        <c:forEach items="${listings}" var="l">
            <tr>
                <td>#${l.id}
                    <c:out value="${l.title}" />
                </td>
                <td>${fn:escapeXml(vi:label(l.status))}</td>
                <td>
                    <c:out value="${l.rejectionReason}" />
                </td>
                <td>
                    <form method="post" action="">
                        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                        <input type="hidden" name="action" value="submit">
                        <input type="hidden" name="id" value="${l.id}">
                        <button class="button">Đăng bán</button>
                    </form><a href="?editListing=${l.id}">Sửa tin đăng</a> · <a href="?images=${l.id}">Sắp xếp ảnh</a>
                </td>
            </tr>
        </c:forEach>
    </table>
</div>
<section class="panel">
    <h2>Ảnh tin đăng</h2>
    <form method="post" enctype="multipart/form-data" action="">
        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
        <input type="hidden" name="action" value="upload">
        <label>Tin đăng
            <select class="form-select" name="id" required>
                <c:forEach items="${listings}" var="x">
                    <option value="${x.id}">
                        <c:out value="${x.title}" /> (#${x.id})</option>
                    </c:forEach>
            </select>
        </label>
        <label>Chọn tối đa 4 ảnh (mỗi ảnh 5 MB). Thay thế bộ ảnh hiện tại.
            <input class="form-control" type="file" name="photos" accept="image/png,image/jpeg" multiple required>
        </label>
        <button class="button">Tải ảnh</button>
    </form>
    <c:forEach items="${images}" var="im">
        <div class="cartline"><img width="90" src="${pageContext.request.contextPath}${fn:escapeXml(im.path)}" alt="Ảnh tin">
            <form method="post" action="">
                <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                <input type="hidden" name="action" value="reorder">
                <input type="hidden" name="id" value="${fn:escapeXml(param.images)}">
                <input type="hidden" name="position" value="${im.position}">
                <button class="button">Đưa lên trước</button>
            </form>
        </div>
    </c:forEach>
</section>
<h2>Biến thể</h2>
<table class="table">
    <tr>
        <th>Mã / Sản phẩm</th>
        <th>Quy cách</th>
        <th>Giá</th>
        <th>Tồn</th>
        <th>Trạng thái</th>
        <th></th>
    </tr>
    <c:forEach items="${variants}" var="v">
        <tr>
            <td>#${v.id}
                <c:out value="${v.title}" />
            </td>
            <td>
                <c:out value="${v.optionName}" />
            </td>
            <td>
                <fmt:formatNumber value="${v.price}" />
            </td>
            <td>${v.availableQty}</td>
            <td>${fn:escapeXml(vi:label(v.status))}</td>
            <td>
                <form method="post" action="">
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                    <input type="hidden" name="action" value="retire">
                    <input type="hidden" name="id" value="${v.id}">
                    <input type="hidden" name="listing" value="${v.listingId}">
                    <button class="button">Ngừng bán</button> <a href="?editVariant=${v.id}">Sửa biến thể</a>
                </form>
            </td>
        </tr>
    </c:forEach>
</table>
<section class="panel">
    <h2>Nhập kho theo lô</h2>
    <form method="post" action="">
        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
        <input type="hidden" name="action" value="stock">
        <label>Biến thể
            <select class="form-select" name="variant" required>
                <c:forEach items="${variants}" var="x">
                    <option value="${x.id}">
                        <c:out value="${x.optionName}" /> (#${x.id})</option>
                    </c:forEach>
            </select>
        </label>
        <label>Mã lô
            <input class="form-control" name="batch" type="text" value="" required>
        </label>
        <label>Xuất xứ
            <input class="form-control" name="origin" type="text" value="" required>
        </label>
        <label>Ngày thu hoạch
            <input class="form-control" name="harvest" type="date" value="" required>
        </label>
        <label>Ngày nhập
            <input class="form-control" name="receipt" type="date" value="" required>
        </label>
        <label>Ngày hết hạn
            <input class="form-control" name="expiry" type="date" value="" required>
        </label>
        <label>Nhà cung cấp
            <input class="form-control" name="supplier" type="text" value="" required>
        </label>
        <label>Số lượng đơn vị bán
            <input class="form-control" name="quantity" type="number" value="" required>
        </label>
        <label>Giá vốn / đơn vị (đ)
            <input class="form-control" name="cost" type="number" value="" required>
        </label>
        <button class="button">Nhập kho</button>
    </form>
</section>
<h2>Lịch sử nhập kho</h2>
<table class="table">
    <tr>
        <th>Lô</th>
        <th>Sản phẩm</th>
        <th>Ngày hết hạn</th>
        <th>Nhập / Còn / Giữ</th>
        <th>Giá vốn</th>
        <th>Tổng giá vốn</th>
    </tr>
    <c:forEach items="${batches}" var="b">
        <tr>
            <td>
                <c:out value="${b.batchNo}" />
            </td>
            <td>
                <c:out value="${b.title}" />
            </td>
            <td>${b.expiryDate}</td>
            <td>${b.receivedQty} / ${b.remainingQty} / ${b.reservedQty}</td>
            <td>
                <fmt:formatNumber value="${b.unitCost}" />
            </td>
            <td>
                <fmt:formatNumber value="${b.totalCost}" />
            </td>
        </tr>
    </c:forEach>
</table>
<section class="panel">
    <h2>Tham khảo sản phẩm trên sàn</h2>
    <form method="get">
        <label>Nhập tên sản phẩm
            <input class="form-control" name="q" data-suggestions="${pageContext.request.contextPath}/seller/suggestions" data-product-url="${pageContext.request.contextPath}/product" type="search" value="${fn:escapeXml(param.q)}">
        </label>
        <button class="button">Gợi ý theo giá giảm dần</button>
    </form>
    <c:set var="products" value="${suggestions}" />
    <%@ include file="/WEB-INF/views/cards.jsp" %>
</section>
<script src="${pageContext.request.contextPath}/assets/js/seller-suggestions.js?v=2" defer></script>
<%@ include file="/WEB-INF/views/footer.jsp" %>
