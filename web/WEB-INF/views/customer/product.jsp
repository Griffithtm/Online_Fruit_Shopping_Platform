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


<section class="panel fr-review-card fr-product-reviews">

    <div class="fr-review-heading">
        <h2>Đánh giá từ khách hàng đã mua</h2>
    </div>

    <c:if test="${empty publicReviews}">
        <p class="fr-empty-reviews">
            Chưa có đánh giá cho sản phẩm này.
        </p>
    </c:if>

    <c:forEach items="${publicReviews}" var="rv">

        <article class="fr-review-quote fr-product-review">

            <%-- Người đánh giá và số sao --%>
            <div class="fr-product-review-header">

                <div class="fr-review-user">
                    <strong class="fr-review-author">
                        <c:out value="${rv.full_name}"/>
                    </strong>

                    <time class="fr-review-date">
                        <fmt:formatDate
                            value="${rv.created_at}"
                            pattern="dd/MM/yyyy HH:mm"/>
                    </time>

                    <div class="fr-public-stars"
                         aria-label="${rv.product_rating} trên 5 sao">
                        <c:forEach begin="1" end="5" var="n">
                            <span class="${n <= rv.product_rating ? 'active' : ''}">★</span>
                        </c:forEach>
                    </div>
                </div>

            </div>

            <div class="fr-review-content">
                <c:out value="${empty rv.comment ? '(Không có nhận xét)' : rv.comment}"/>
            </div>
            <%-- Báo cáo đánh giá --%>
            <c:if test="${not empty sessionScope.user}">

                <details class="fr-report-details">

                    <summary>Báo cáo đánh giá này</summary>

                    <form method="post"
                          action="${pageContext.request.contextPath}/reviews"
                          class="fr-report-form">

                        <input type="hidden"
                               name="csrf"
                               value="${sessionScope.csrf}">

                        <input type="hidden"
                               name="action"
                               value="report">

                        <input type="hidden"
                               name="reviewId"
                               value="${rv.id}">

                        <label>
                            Lý do báo cáo

                            <select class="form-control"
                                    name="reason"
                                    required>

                                <option value="">Chọn lý do</option>
                                <option value="SPAM">Spam</option>
                                <option value="OFFENSIVE">Nội dung xúc phạm</option>
                                <option value="MISLEADING">Thông tin sai lệch</option>
                                <option value="OTHER">Khác</option>

                            </select>
                        </label>

                        <label>
                            Mô tả thêm (không bắt buộc)

                            <textarea class="form-control"
                                      name="description"
                                      maxlength="500"
                                      rows="3"></textarea>
                        </label>

                        <button class="button" type="submit">
                            Gửi báo cáo
                        </button>

                    </form>

                </details>

            </c:if>

            <c:if test="${empty sessionScope.user}">
                <a class="fr-login-report"
                   href="${pageContext.request.contextPath}/login">
                    Đăng nhập để báo cáo đánh giá
                </a>
            </c:if>

        </article>

    </c:forEach>

</section>


<h2>Các lựa chọn cùng danh mục</h2>
<c:set var="products" value="${similar}" />
<%@ include file="/WEB-INF/views/cards.jsp" %>
<%@ include file="/WEB-INF/views/footer.jsp" %>