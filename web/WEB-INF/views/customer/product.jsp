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
                <p>Chỉ hiển thị đánh giá từ các đơn hàng đã giao. Báo cáo sẽ được Admin xem xét.</p>
            </div>
            <c:if test="${empty publicReviews}"><p>Chưa có đánh giá cho sản phẩm này.</p></c:if>
            <c:forEach items="${publicReviews}" var="rv">
                <article class="fr-review-quote fr-product-review">
                    <div class="fr-review-meta">
                        <strong><c:out value="${rv.full_name}"/></strong>
                        <span>Sản phẩm: <strong><c:out value="${rv.product_rating}"/>/5</strong></span>
                        <span>Người bán: <strong><c:out value="${rv.seller_rating}"/>/5</strong></span>
                        <small><c:out value="${rv.created_at}"/></small>
                    </div>
                    <p><c:out value="${empty rv.comment ? '(Không có nhận xét)' : rv.comment}"/></p>
                    <c:if test="${not empty sessionScope.user}">
                        <details class="fr-report-details">
                            <summary>Báo cáo đánh giá này</summary>
                            <form method="post" action="${pageContext.request.contextPath}/reviews" class="fr-report-form">
                                <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                                <input type="hidden" name="action" value="report">
                                <input type="hidden" name="reviewId" value="${rv.id}">
                                <label>Lý do báo cáo
                                    <select class="form-control" name="reason" required>
                                        <option value="">Chọn lý do</option>
                                        <option value="SPAM">Spam</option>
                                        <option value="OFFENSIVE">Nội dung xúc phạm</option>
                                        <option value="MISLEADING">Thông tin sai lệch</option>
                                        <option value="OTHER">Khác</option>
                                    </select>
                                </label>
                                <label>Mô tả thêm (không bắt buộc)
                                    <textarea class="form-control" name="description" maxlength="500" rows="2"></textarea>
                                </label>
                                <button class="button" type="submit">Gửi báo cáo</button>
                            </form>
                        </details>
                    </c:if>
                    <c:if test="${empty sessionScope.user}">
                        <a href="${pageContext.request.contextPath}/login">Đăng nhập để báo cáo đánh giá</a>
                    </c:if>
                </article>
            </c:forEach>
        </section>

        <h2>Các lựa chọn cùng danh mục</h2>
        <c:set var="products" value="${similar}" />
        <%@ include file="/WEB-INF/views/cards.jsp" %>
            <%@ include file="/WEB-INF/views/footer.jsp" %>