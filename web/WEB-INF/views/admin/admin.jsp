<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/header.jsp" %>
<div class="container py-4">
    <h1>Quản trị Freshora</h1>
    <c:if test="${not empty error}"><div class="alert alert-danger"><c:out value="${error}"/></div></c:if>
    <c:if test="${param.saved == '1'}"><div class="alert alert-success">Đã cập nhật thành công.</div></c:if>

    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3">
        <h2>Tin đăng chờ duyệt</h2>
        <a class="button" href="${pageContext.request.contextPath}/admin/listing-reviews">Mở trang hậu kiểm sản phẩm</a>
    </div>
    <p class="text-muted">Danh sách này chỉ gồm sản phẩm bị đình chỉ đã được Seller chỉnh sửa và gửi yêu cầu duyệt lại.</p>
    <c:forEach items="${pending}" var="l">
        <section class="panel p-3 mb-3">
            <div class="d-flex gap-3 align-items-start">
                <c:if test="${not empty l.image}"><img src="${pageContext.request.contextPath}${fn:escapeXml(l.image)}" alt="" style="width:100px;height:100px;object-fit:cover;border-radius:8px"></c:if>
                <div class="flex-grow-1">
                    <h3>#${l.id} <c:out value="${l.title}"/></h3>
                    <p><strong>Cửa hàng:</strong> <c:out value="${l.storeName}"/></p>
                    <p><c:out value="${l.description}"/></p>
                    <a href="${pageContext.request.contextPath}/product?id=${l.id}" target="_blank" rel="noopener">Xem chi tiết sản phẩm ↗</a>
                    <p class="small text-muted">Gửi duyệt lúc: <fmt:formatDate value="${l.updatedAt}" pattern="dd/MM/yyyy HH:mm"/></p>
                </div>
            </div>
            <div class="d-flex flex-wrap gap-2 mt-2">
                <form method="post" action="${pageContext.request.contextPath}/admin" onsubmit="return confirm('Duyệt và công khai sản phẩm này?');">
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="review"><input type="hidden" name="id" value="${l.id}"><input type="hidden" name="status" value="APPROVED">
                    <button class="btn btn-success">Duyệt và công khai</button>
                </form>
                <form method="post" action="${pageContext.request.contextPath}/admin" class="d-flex flex-grow-1 gap-2" onsubmit="return confirm('Từ chối yêu cầu duyệt lại?');">
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="review"><input type="hidden" name="id" value="${l.id}"><input type="hidden" name="status" value="REJECTED">
                    <input class="form-control" name="reason" maxlength="500" placeholder="Lý do từ chối (bắt buộc)" required>
                    <button class="btn btn-danger">Từ chối</button>
                </form>
            </div>
        </section>
    </c:forEach>
    <c:if test="${empty pending}"><div class="panel p-4 text-muted">Hiện không có sản phẩm nào chờ duyệt lại.</div></c:if>

    <h2 class="mt-4">Phân quyền tài khoản</h2>
    <p>Người bán đăng ký riêng cùng cửa hàng. Không có màn hình duyệt người bán.</p>
    <div class="table-responsive"><table class="table"><tr><th>Tài khoản</th><th>Vai trò</th><th>Cập nhật</th></tr>
        <c:forEach items="${users}" var="u"><tr><td><c:out value="${u.email}"/></td><td>${fn:escapeXml(vi:label(u.role))}</td><td>
            <form method="post" action="${pageContext.request.contextPath}/admin"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="role"><input type="hidden" name="id" value="${u.id}">
                <select class="form-select" name="role"><option value="CUSTOMER">Khách hàng</option><option value="DELIVERY_STAFF">Nhân viên giao hàng</option><option value="ADMIN">Quản trị viên</option></select><button class="button mt-1">Đổi quyền</button>
            </form></td></tr></c:forEach>
    </table></div>
</div>
<%@ include file="/WEB-INF/views/footer.jsp" %>
