<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/header.jsp" %>
<section class="fr-review-page">
    <div class="fr-review-heading"><span class="eyebrow">FRESHORA COMMUNITY</span><h1>Đánh giá của tôi</h1><p>Chia sẻ trải nghiệm thực tế sau khi đơn hàng được giao thành công.</p></div>
    <c:if test="${not empty error}"><div class="alert alert-danger"><c:out value="${error}"/></div></c:if>
    <c:if test="${param.saved == '1'}"><div class="alert alert-success">Thao tác đã được thực hiện.</div></c:if>
    <c:if test="${empty items}"><div class="panel">Bạn chưa có sản phẩm nào từ đơn hàng đã giao đủ điều kiện đánh giá.</div></c:if>
    <c:forEach items="${items}" var="i">
        <article class="panel fr-review-card">
            <div class="fr-review-product"><div><span class="eyebrow">ĐƠN HÀNG ĐÃ GIAO</span><h2><c:out value="${i.title}"/></h2><p><c:out value="${i.option_name}"/> · <c:out value="${i.quantity}"/> <c:out value="${i.unit}"/> · Giao lúc <c:out value="${i.delivered_at}"/></p></div>
                <c:choose><c:when test="${empty i.review_id}"><span class="fr-review-status">Chưa đánh giá</span></c:when><c:otherwise><span class="fr-review-status">${i.moderation_status == 'REPORTED' ? 'Đang được xem xét' : (i.moderation_status == 'HIDDEN' ? 'Đã ẩn' : (i.moderation_status == 'REMOVED' ? 'Đã gỡ' : 'Đã đánh giá'))}</span></c:otherwise></c:choose>
                    </div>
            <c:choose><c:when test="${empty i.review_id || (i.moderation_status != 'HIDDEN' && i.moderation_status != 'REMOVED')}">
                    <form method="post" action="${pageContext.request.contextPath}/reviews" class="fr-review-form">
                        <input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="save"><input type="hidden" name="itemId" value="${i.item_id}">
                        <div class="row g-3">

                            <%-- Đánh giá sản phẩm --%>
                            <div class="col-md-6">
                                <label class="form-label">Đánh giá sản phẩm</label>

                                <div class="star-rating" data-rating-group>
                                    <c:forEach begin="1" end="5" var="n">
                                        <label class="star-option">
                                            <input
                                                type="radio"
                                                name="productRating"
                                                value="${n}"
                                                data-rating-value="${n}"
                                                ${i.product_rating == n ? 'checked' : ''}
                                                required>

                                            <span class="star-icon">★</span>
                                        </label>
                                    </c:forEach>
                                </div>
                            </div>

                            <%-- Đánh giá người bán --%>
                            <div class="col-md-6">
                                <label class="form-label">Đánh giá người bán</label>

                                <div class="star-rating" data-rating-group>
                                    <c:forEach begin="1" end="5" var="n">
                                        <label class="star-option">
                                            <input
                                                type="radio"
                                                name="sellerRating"
                                                value="${n}"
                                                data-rating-value="${n}"
                                                ${i.seller_rating == n ? 'checked' : ''}
                                                required>

                                            <span class="star-icon">★</span>
                                        </label>
                                    </c:forEach>
                                </div>
                            </div>

                        </div>
                        <label class="form-label mt-3">Nhận xét (không bắt buộc)</label><textarea class="form-control" name="comment" rows="3" maxlength="2000" placeholder="Chất lượng, độ tươi, đóng gói..."><c:out value="${i.comment}"/></textarea>
                        <button class="button mt-3" type="submit">${empty i.review_id ? 'Đăng đánh giá' : 'Cập nhật đánh giá'}</button>
                    </form>
                    <c:if test="${not empty i.review_id}"><form method="post" action="${pageContext.request.contextPath}/reviews" class="d-inline" onsubmit="return confirm('Bạn chắc chắn muốn xóa đánh giá?')"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="delete"><input type="hidden" name="reviewId" value="${i.review_id}"><button class="btn btn-outline-secondary mt-3" type="submit">Xóa đánh giá</button></form></c:if>
                </c:when><c:otherwise><p class="mb-0">Đánh giá này đã bị ẩn hoặc gỡ sau khi quản trị viên xem xét.</p></c:otherwise></c:choose>
                </article>
    </c:forEach>
</section>
<script>
    document.querySelectorAll('[data-rating-group]').forEach(group => {

        const inputs = group.querySelectorAll('input[type="radio"]');

        function updateStars() {
            const selected = group.querySelector('input:checked');
            const selectedValue = selected
                    ? Number(selected.value)
                    : 0;

            group.querySelectorAll('.star-option').forEach(label => {
                const input = label.querySelector('input');
                const value = Number(input.value);

                label.classList.toggle(
                        'active',
                        value <= selectedValue
                        );
            });
        }

        inputs.forEach(input => {
            input.addEventListener('change', updateStars);
        });

        // Hiển thị đúng số sao đã lưu trước đó
        updateStars();
    });
</script>
<%@ include file="/WEB-INF/views/footer.jsp" %>
