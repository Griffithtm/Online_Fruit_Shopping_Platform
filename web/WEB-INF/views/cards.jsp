<%@ page pageEncoding="UTF-8" %>
    <div class="row g-4">
        <c:forEach items="${products}" var="p">
            <div class="col-sm-6 col-lg-4">
                <article class="mcard">
                    <a class="mimg d-block" href="${pageContext.request.contextPath}/product?id=${p.listing_id}"><img src="${pageContext.request.contextPath}${empty p.image ? '/assets/img/fruit.svg' : fn:escapeXml(p.image)}" alt="${fn:escapeXml(p.title)}">
                        <div class="mbdg new"><i class="fas fa-leaf"></i>Available</div>
                    </a>
                    <div class="mbody">
                        <a class="mcat" href="${pageContext.request.contextPath}/shop?id=${p.store_id}">
                            <c:out value="${p.store_name}" />
                        </a><a href="${pageContext.request.contextPath}/product?id=${p.listing_id}"><h3 class="mtit"><c:out value="${p.title}"/></h3></a>
                        <div class="mdesc">
                            <c:out value="${p.option_name}" /> ·
                            <c:out value="${p.origin}" />
                        </div>
                        <div class="mfoot">
                            <div>
                                <div class="mprice">
                                    <fmt:formatNumber value="${p.price}" pattern="#,##0" /> <small>VND</small>
                                </div>
                                <div class="mstars"><i class="fas fa-star"></i>
                                    <c:out value="${p.rating}" /> <span class="stock-label">· ${p.available_qty} available</span>
                                </div>
                            </div><a class="madd d-flex align-items-center justify-content-center" href="${pageContext.request.contextPath}/product?id=${p.listing_id}" aria-label="View ${fn:escapeXml(p.title)}"><i class="fas fa-plus"></i></a>
                        </div>
                    </div>
                </article>
            </div>
        </c:forEach>
    </div>
    <c:if test="${empty products}">
        <div class="empty">No matching products. Try different search filters.</div>
    </c:if>