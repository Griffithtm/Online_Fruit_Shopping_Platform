<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <h1>Stores on Freshora</h1>
        <div class="productgrid">
            <c:forEach items="${shops}" var="s">
                <article class="panel">
                    <h2><c:out value="${s.store_name}"/></h2>
                    <p>
                        <c:out value="${s.description}" />
                    </p>
                    <p>Rating ${s.rating}/5</p><a class="button" href="${pageContext.request.contextPath}/shop?id=${s.id}">View store</a>
                </article>
            </c:forEach>
        </div>
        <%@ include file="/WEB-INF/views/footer.jsp" %>