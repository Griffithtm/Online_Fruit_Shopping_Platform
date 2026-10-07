<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <h1>Your cart</h1>
        <p>Adding items to your cart does not reserve stock. Prices and quantities will be checked again when you place an order.</p>
        <c:if test="${empty lines}">
            <div class="empty">Your cart is empty. <a href="${pageContext.request.contextPath}/products">Explore products</a>
            </div>
        </c:if>
        <c:forEach items="${totals}" var="shop">
            <section class="panel">
                <h2><c:out value="${shop.key}"/></h2>
                <c:forEach items="${lines}" var="l">
                    <c:if test="${l.store_name == shop.key}">
                        <div class="cartline">
                            <div>
                                <h3><c:out value="${l.title}"/></h3>
                                <c:if test="${l.purchasable == 0}"><p role="alert">This product is no longer available. Please remove it.</p></c:if>
                                <c:if test="${l.quantity > l.available_qty}"><p role="alert">Stock has changed. Reduce the quantity before checkout.</p></c:if>
                                <c:if test="${l.saved_price != l.unit_price}"><p role="alert">The price has changed from <fmt:formatNumber value="${l.saved_price}"/> VND. Click Update to accept the current price.</p></c:if>
                                <c:out value="${l.option_name}" /> ·
                                <fmt:formatNumber value="${l.unit_price}" /> VND · Available ${l.available_qty}</div>
                            <form method="post" action="">
                                <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                                <input type="hidden" name="action" value="update">
                                <input type="hidden" name="id" value="${l.id}">
                                <label>Quantity
                                    <input class="form-control" name="quantity" type="number" value="${l.quantity}" required>
                                </label>
                                <button class="button">Update</button>
                            </form>
                            <form method="post" action="">
                                <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                                <input type="hidden" name="action" value="remove">
                                <input type="hidden" name="id" value="${l.id}">
                                <button class="button">Delete</button>
                            </form><strong><fmt:formatNumber value="${l.line_total}"/> VND</strong>
                        </div>
                    </c:if>
                </c:forEach>
                <p>Store subtotal: <strong><fmt:formatNumber value="${shop.value}"/> VND</strong>
                </p>
            </section>
        </c:forEach>
        <h2>Subtotal: <fmt:formatNumber value="${total}"/> VND</h2>
        <c:if test="${not empty lines}"><a class="button" href="${pageContext.request.contextPath}/checkout">Proceed to checkout</a></c:if><p><a href="${pageContext.request.contextPath}/orders">My orders</a></p>
        <%@ include file="/WEB-INF/views/footer.jsp" %>