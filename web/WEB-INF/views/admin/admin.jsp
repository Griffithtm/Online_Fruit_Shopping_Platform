<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <h1>Quản trị Freshora</h1>
        <h2>Tin đăng chờ duyệt</h2>
        <c:forEach items="${pending}" var="l">
            <section class="panel">
                <h3>#${l.id} <c:out value="${l.title}"/></h3>
                <p>
                    <c:out value="${l.storeName}" /> ·
                    <c:out value="${l.description}" />
                </p>
                <form method="post" action="">
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                    <input type="hidden" name="action" value="review">
                    <input type="hidden" name="id" value="${l.id}">
                    <select class="form-select" name="status">
                        <option value="APPROVED">Duyệt tin</option>
                        <option value="REJECTED">Từ chối</option>
                    </select>
                    <label>Lý do nếu từ chối
                        <input class="form-control" name="reason" type="text" value="">
                    </label>
                    <button class="button">Xác nhận</button>
                </form>
            </section>
        </c:forEach>
        <h2>Phân quyền tài khoản</h2>
        <p>người bán đăng ký riêng cùng cửa hàng. Không có màn hình duyệt người bán.</p>
        <table class="table">
            <tr>
                <th>Tài khoản</th>
                <th>Vai trò</th>
                <th>Cập nhật</th>
            </tr>
            <c:forEach items="${users}" var="u">
                <tr>
                    <td>
                        <c:out value="${u.email}" />
                    </td>
                    <td>${fn:escapeXml(vi:label(u.role))}</td>
                    <td>
                        <form method="post" action="">
                            <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                            <input type="hidden" name="action" value="role">
                            <input type="hidden" name="id" value="${u.id}">
                            <select class="form-select" name="role">
                                <option value="CUSTOMER">Khách hàng</option>
                                <option value="DELIVERY_STAFF">Nhân viên giao hàng</option>
                                <option value="ADMIN">Quản trị viên</option>
                            </select>
                            <button class="button">Đổi quyền</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
        </table>
        <%@ include file="/WEB-INF/views/footer.jsp" %>