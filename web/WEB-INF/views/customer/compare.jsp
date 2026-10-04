<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <h1>Compare offers from stores</h1>
        <p>Compare by name, category, pack size, and selling unit. Delivery fees will be calculated at checkout in Iteration 2.</p>
        <form class="filters" method="get">
            <label>Search products
                <input class="form-control" name="q" value="${fn:escapeXml(param.q)}" placeholder="Oranges, mangoes, apples...">
            </label>
            <label>Category
                <select class="form-select" name="category">
                    <option value="">All</option>
                    <c:forEach items="${categories}" var="x">
                        <option value="${x.id}" ${param.category==x .id ? 'selected' : ''}>
                            <c:out value="${x.name}" />
                        </option>
                    </c:forEach>
                </select>
            </label>
            <label>Minimum price
                <input class="form-control" type="number" min="0" name="min" value="${fn:escapeXml(param.min)}">
            </label>
            <label>Maximum price
                <input class="form-control" type="number" min="0" name="max" value="${fn:escapeXml(param.max)}">
            </label>
            <label>Minimum rating
                <select class="form-select" name="rating">
                    <option value="">All</option>
                    <option value="4">4 stars</option>
                    <option value="3">3 stars</option>
                </select>
            </label>
            <label>Sort by
                <select class="form-select" name="sort">
                    <option value="new">Newest</option>
                    <option value="priceAsc">Price: low to high</option>
                    <option value="priceDesc">Price: high to low</option>
                    <option value="rating">Rating</option>
                    <option value="popular">Popularity</option>
                </select>
            </label>
            <label>
                <input type="checkbox" name="promo" value="1"> On promotion</label>
            <button class="button">Search</button>
        </form>
        <div class="table-responsive">
            <table class="table">
                <tr>
                    <th>Product</th>
                    <th>Store</th>
                    <th>Pack size / Unit</th>
                    <th>Price</th>
                    <th>Store rating</th>
                    <th>Stock</th>
                </tr>
                <c:forEach items="${products}" var="p">
                    <tr>
                        <td>
                            <a href="${pageContext.request.contextPath}/product?id=${p.listing_id}">
                                <c:out value="${p.title}" />
                            </a>
                        </td>
                        <td>
                            <c:out value="${p.store_name}" />
                        </td>
                        <td>
                            <c:out value="${p.option_name}" /> /
                            <c:out value="${p.unit}" />
                        </td>
                        <td>
                            <fmt:formatNumber value="${p.price}" /> VND</td>
                        <td>${p.seller_rating}/5</td>
                        <td>${p.available_qty}</td>
                    </tr>
                </c:forEach>
            </table>
        </div>
        <%@ include file="/WEB-INF/views/footer.jsp" %>