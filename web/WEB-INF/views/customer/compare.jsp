<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <h1>Compare offers from stores</h1>
        <p>Compare matching fruit names, origin and pack sizes before choosing an offer. Prices per kg are calculated only for packs with an explicit weight (for example, 500 g or 1 kg).</p>
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
            <label>Availability <select class="form-select" name="availability">
<option value="in" ${empty param.availability or param.availability == 'in' ? 'selected' : ''}>In stock</option>
<option value="out" ${param.availability == 'out' ? 'selected' : ''}>Out of stock</option>
<option value="all" ${param.availability == 'all' ? 'selected' : ''}>All</option></select></label>
<c:if test="${empty shop}"><label>Seller <select class="form-select" name="store"><option value="">All stores</option><c:forEach items="${shops}" var="seller"><option value="${seller.id}" ${param.store == seller.id ? 'selected' : ''}><c:out value="${seller.store_name}"/></option></c:forEach></select></label></c:if>
<label>Minimum price
                <input class="form-control" type="number" min="0" name="min" value="${fn:escapeXml(param.min)}">
            </label>
            <label>Maximum price
                <input class="form-control" type="number" min="0" name="max" value="${fn:escapeXml(param.max)}">
            </label>
            <label>Minimum rating
                <select class="form-select" name="rating">
                    <option value="">All</option>
                    <option value="4" ${param.rating == '4' ? 'selected' : ''}>4 stars</option>
                    <option value="3" ${param.rating == '3' ? 'selected' : ''}>3 stars</option>
                </select>
            </label>
            <label>Sort by
                <select class="form-select" name="sort">
                    <option value="unitPrice" ${param.sort == 'unitPrice' ? 'selected' : ''}>VND/kg: ascending</option><option value="new" ${param.sort == 'new' ? 'selected' : ''}>Newest</option>
                    <option value="priceAsc" ${param.sort == 'priceAsc' ? 'selected' : ''}>Price: low to high</option>
                    <option value="priceDesc" ${param.sort == 'priceDesc' ? 'selected' : ''}>Price: high to low</option>
                    <option value="rating" ${param.sort == 'rating' ? 'selected' : ''}>Rating</option>
                    <option value="popular" ${param.sort == 'popular' ? 'selected' : ''}>Popularity</option>
                </select>
            </label>
            <label>
                <input type="checkbox" name="promo" value="1" ${param.promo == '1' ? 'checked' : ''}> On promotion</label>
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
                    <th>Stock</th><th>Price per kg</th><th>Origin</th><th>Delivery terms</th>
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
                        <td>${p.available_qty}</td><td><c:choose><c:when test="${not empty p.unit_price_kg}"><fmt:formatNumber value="${p.unit_price_kg}"/> VND/kg</c:when><c:otherwise>Weight not specified</c:otherwise></c:choose></td><td><c:out value="${p.origin}"/></td><td>Platform delivery; see service areas below.</td>
                    </tr>
                </c:forEach>
            </table>
        </div>
        <section class="panel"><h2>Delivery service areas</h2><p>These platform delivery terms apply to all sellers. The final fee and estimated arrival are confirmed for your address at checkout.</p>
<c:forEach items="${deliveryAreas}" var="area"><p><c:out value="${area.name}"/>: <c:choose><c:when test="${not empty area.delivery_fee}"><fmt:formatNumber value="${area.delivery_fee}"/> VND</c:when><c:otherwise>Fee confirmed at checkout</c:otherwise></c:choose></p></c:forEach>
<c:if test="${empty deliveryAreas}"><p>No delivery service areas are currently configured.</p></c:if></section>
<%@ include file="/WEB-INF/views/footer.jsp" %>