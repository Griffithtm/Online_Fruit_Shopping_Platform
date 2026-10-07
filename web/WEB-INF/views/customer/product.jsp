<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <div class="detailgrid"><img class="detailimage" src="${pageContext.request.contextPath}${empty product.image ? '/assets/img/fruits/fruit-basket.jpg' : fn:escapeXml(product.image)}" alt="Fruit">
            <div><span class="eyebrow"><c:out value="${product.origin}"/></span>
                <h1><c:out value="${product.title}"/></h1>
                <p>
                    <c:out value="${product.description}" />
                </p>
                <p>
                    <c:out value="${product.harvest_info}" />
                </p>
                <a href="${pageContext.request.contextPath}/shop?id=${product.store_id}">
                    <c:out value="${product.store_name}" /> · ${product.seller_rating}/5</a>
                <c:forEach items="${variants}" var="v">
                    <div class="panel">
                        <h3><c:out value="${v.option_name}"/></h3><strong><fmt:formatNumber value="${v.price}"/> VND / <c:out value="${v.unit}"/></strong>
                        <p>Available ${v.available_qty}</p>
                        <form method="post" action="${pageContext.request.contextPath}/cart">
                            <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                            <input type="hidden" name="action" value="add">
                            <input type="hidden" name="id" value="${v.id}">
                            <label>Quantity
                                <input class="form-control" name="quantity" type="number" min="1" max="${v.available_qty}" value="1" required>
                            </label>
                            <button class="button" ${v.available_qty le 0 ? 'disabled' : ''}>${v.available_qty le 0 ? 'Out of stock' : 'Add to cart'}</button>
                        </form>
                    </div>
                </c:forEach>
            </div>
        </div>
        <h2>More from this category</h2>
        <c:set var="products" value="${similar}" />
        <%@ include file="/WEB-INF/views/cards.jsp" %>
            <%@ include file="/WEB-INF/views/footer.jsp" %>