<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <section class="authbox panel"><span class="eyebrow">CHÀO MỪNG ĐẾN FRESHORA</span>
            <h1><c:choose><c:when test="${mode == '/login'}">Đăng nhập</c:when><c:when test="${mode == '/forgot'}">Quên mật khẩu</c:when><c:when test="${mode == '/reset'}">Đặt lại mật khẩu</c:when><c:when test="${mode == '/register/seller'}">Đăng ký bán hàng</c:when><c:otherwise>Đăng ký khách hàng</c:otherwise></c:choose></h1>
            <form method="post" action="">
                <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                <c:if test="${mode != '/reset'}">
                    <label>Thư điện tử
                        <input class="form-control" name="email" type="email" value="" required>
                    </label>
                </c:if>
                <c:if test="${mode != '/forgot'}">
                    <label>Mật khẩu
                        <input class="form-control" name="password" type="password" value="" required>
                    </label>
                </c:if>
                <c:if test="${mode == '/reset' || fn:startsWith(mode,'/register')}">
                    <label>Nhập lại mật khẩu
                        <input class="form-control" name="confirm" type="password" value="" required>
                    </label>
                </c:if>
                <c:if test="${fn:startsWith(mode,'/register')}">
                    <label>Họ tên
                        <input class="form-control" name="name" type="text" value="" required>
                    </label>
                    <label>Số điện thoại
                        <input class="form-control" name="phone" type="tel" value="" required>
                    </label>
                </c:if>
                <c:if test="${mode == '/register/seller'}">
                    <label>Tên cửa hàng
                        <input class="form-control" name="store" type="text" value="" required>
                    </label>
                    <p>Cửa hàng hoạt động ngay sau đăng ký. Không cần tài khoản khách hàng trước đó.</p>
                </c:if>
                <input type="hidden" name="token" value="${fn:escapeXml(param.token)}">
                <button class="button">Tiếp tục</button>
            </form>
            <hr><a href="${pageContext.request.contextPath}/forgot">Quên mật khẩu?</a> · <a href="${pageContext.request.contextPath}/register/customer">Khách hàng mới</a> · <a href="${pageContext.request.contextPath}/register/seller">Đăng ký bán hàng</a>
        </section>
        <%@ include file="/WEB-INF/views/footer.jsp" %>