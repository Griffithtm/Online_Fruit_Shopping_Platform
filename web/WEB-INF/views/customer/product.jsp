<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <div class="detailgrid"><img class="detailimage" src="${pageContext.request.contextPath}${empty product.image ? '/assets/img/fruits/fruit-basket.jpg' : fn:escapeXml(product.image)}" alt="Trái cây">
            <div><span class="eyebrow"><c:out value="${product.origin}"/></span>
                <h1><c:out value="${product.title}"/></h1>
                <p>
                    <c:out value="${product.description}" />
                </p>
                <p>
                    <c:out value="${product.harvestInfo}" />
                </p>
                <a href="${pageContext.request.contextPath}/shop?id=${product.storeId}">
                    <c:out value="${product.storeName}" /> · ${product.sellerRating}/5</a>
                <c:forEach items="${variants}" var="v">
                    <div class="panel">
                        <h3><c:out value="${v.optionName}"/></h3><strong><fmt:formatNumber value="${v.price}"/> đ / <c:out value="${v.unit}"/></strong>
                        <p>Còn ${v.availableQty}</p>
                        <form method="post" action="${pageContext.request.contextPath}/cart">
                            <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                            <input type="hidden" name="action" value="add">
                            <input type="hidden" name="id" value="${v.id}">
                            <label>Số lượng
                                <input class="form-control" name="quantity" type="number" min="1" max="${v.availableQty}" value="1" required>
                            </label>
                            <button class="button" ${v.availableQty le 0 ? 'disabled' : ''}>${v.availableQty le 0 ? 'Hết hàng' : 'Thêm vào giỏ'}</button>
                        </form>
                    </div>
                </c:forEach>
            </div>
        </div>
        <h2>Các lựa chọn cùng danh mục</h2>
        <c:set var="products" value="${similar}" />
        <%@ include file="/WEB-INF/views/cards.jsp" %>
            <%@ include file="/WEB-INF/views/footer.jsp" %>