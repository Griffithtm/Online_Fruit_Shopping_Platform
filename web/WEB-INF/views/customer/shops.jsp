<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <h1>Cửa hàng trên Freshora</h1>
        <div class="productgrid">
            <c:forEach items="${shops}" var="s">
                <article class="panel">
                    <h2><c:out value="${s.storeName}"/></h2>
                    <p>
                        <c:out value="${s.description}" />
                    </p>
                    <p>Đánh giá ${s.rating}/5</p><a class="button" href="${pageContext.request.contextPath}/shop?id=${s.id}">Xem cửa hàng</a>
                </article>
            </c:forEach>
        </div>
        <%@ include file="/WEB-INF/views/footer.jsp" %>