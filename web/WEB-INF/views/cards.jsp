<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page pageEncoding="UTF-8" %>
    <div class="row g-4">
        <c:forEach items="${products}" var="p">
            <div class="col-sm-6 col-lg-4">
                <article class="mcard">
                    <a class="mimg d-block" href="${pageContext.request.contextPath}/product?id=${p.listingId}"><img src="${pageContext.request.contextPath}${empty p.image ? '/assets/img/fruits/fruit-basket.jpg' : fn:escapeXml(p.image)}" alt="${fn:escapeXml(p.title)}">
                        <div class="mbdg new"><i class="fas fa-leaf"></i>${p.availableQty > 0 ? 'Còn hàng' : 'Hết hàng'}</div>
                    </a>
                    <div class="mbody">
                        <a class="mcat" href="${pageContext.request.contextPath}/shop?id=${p.storeId}">
                            <c:out value="${p.storeName}" />
                        </a><a href="${pageContext.request.contextPath}/product?id=${p.listingId}"><h3 class="mtit"><c:out value="${p.title}"/></h3></a>
                        <div class="mdesc">
                            <c:out value="${p.optionName}" /> ·
                            <c:out value="${p.origin}" />
                        </div>
                        <div class="mfoot">
                            <div>
                                <div class="mprice">
                                    <fmt:formatNumber value="${p.price}" pattern="#,##0" /> <small>đ</small>
                                </div>
                                <div class="mstars"><i class="fas fa-star"></i>
                                    <c:out value="${p.rating}" /> <span class="stock-label">· Còn ${p.availableQty}</span>
                                </div>
                            </div><a class="madd d-flex align-items-center justify-content-center" href="${pageContext.request.contextPath}/product?id=${p.listingId}" aria-label="Xem ${fn:escapeXml(p.title)}"><i class="fas fa-plus"></i></a>
                        </div>
                    </div>
                </article>
            </div>
        </c:forEach>
    </div>
    <c:if test="${empty products}">
        <div class="empty">Chưa có sản phẩm phù hợp. Hãy thử điều kiện tìm kiếm khác.</div>
    </c:if>