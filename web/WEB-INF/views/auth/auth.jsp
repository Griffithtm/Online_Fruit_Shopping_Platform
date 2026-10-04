<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <section class="authbox panel"><span class="eyebrow">WELCOME TO FRESHORA</span>
            <h1><c:choose><c:when test="${mode == '/login'}">Log in</c:when><c:when test="${mode == '/forgot'}">Forgot password</c:when><c:when test="${mode == '/reset'}">Reset password</c:when><c:when test="${mode == '/register/seller'}">Register as a Seller</c:when><c:otherwise>Register as a Customer</c:otherwise></c:choose></h1>
            <form method="post" action="">
                <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                <c:if test="${mode != '/reset'}">
                    <label>Email
                        <input class="form-control" name="email" type="email" value="" required>
                    </label>
                </c:if>
                <c:if test="${mode != '/forgot'}">
                    <label>Password
                        <input class="form-control" name="password" type="password" value="" required>
                    </label>
                </c:if>
                <c:if test="${mode == '/reset' || fn:startsWith(mode,'/register')}">
                    <label>Confirm password
                        <input class="form-control" name="confirm" type="password" value="" required>
                    </label>
                </c:if>
                <c:if test="${fn:startsWith(mode,'/register')}">
                    <label>Full name
                        <input class="form-control" name="name" type="text" value="" required>
                    </label>
                    <label>Phone number
                        <input class="form-control" name="phone" type="tel" value="" required>
                    </label>
                </c:if>
                <c:if test="${mode == '/register/seller'}">
                    <label>Store name
                        <input class="form-control" name="store" type="text" value="" required>
                    </label>
                    <p>Your store is active immediately after registration. No existing Customer account is required.</p>
                </c:if>
                <input type="hidden" name="token" value="${fn:escapeXml(param.token)}">
                <button class="button">Continue</button>
            </form>
            <hr><a href="${pageContext.request.contextPath}/forgot">Forgot password?</a> · <a href="${pageContext.request.contextPath}/register/customer">New customer</a> · <a href="${pageContext.request.contextPath}/register/seller">Register as a Seller</a>
        </section>
        <%@ include file="/WEB-INF/views/footer.jsp" %>