<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <h1><c:out value="${empty shop ? 'Explore products' : shop.store_name}"/></h1>
        <p>
            <c:out value="${shop.description}" />
        </p>
        <form class="filters" method="get">
            <c:if test="${not empty shop}">
                <input type="hidden" name="id" value="${shop.id}">
            </c:if>
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
        <%@ include file="/WEB-INF/views/cards.jsp" %>
            <%@ include file="/WEB-INF/views/footer.jsp" %>