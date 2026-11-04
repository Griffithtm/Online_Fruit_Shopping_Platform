<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/header.jsp" %>
<section class="fr-review-page">
 <div class="fr-review-heading"><span class="eyebrow">ADMIN MODERATION</span><h1>Báo cáo đánh giá</h1><p>Chỉ đánh giá có báo cáo đang mở mới xuất hiện trong hàng đợi.</p></div>
 <c:if test="${not empty error}"><div class="alert alert-danger"><c:out value="${error}"/></div></c:if><c:if test="${param.saved == '1'}"><div class="alert alert-success">Đã hoàn tất kiểm duyệt và đóng các báo cáo đang mở.</div></c:if>
 <c:if test="${empty reports}"><div class="panel">Không có báo cáo nào đang chờ xử lý.</div></c:if>
 <c:forEach items="${reports}" var="r"><article class="panel fr-review-card">
  <div class="fr-review-product"><div><span class="eyebrow">${r.open_count} BÁO CÁO ĐANG MỞ</span><h2><c:out value="${r.title}"/></h2><p>Đánh giá #${r.review_id} · Người đánh giá: <c:out value="${r.author_name}"/></p></div><span class="fr-review-status">${r.moderation_status}</span></div>
  <blockquote class="fr-review-quote"><c:out value="${r.comment}" default="(Không có nhận xét)"/></blockquote>
  <p><strong>Lý do:</strong> <c:out value="${r.reason}"/> · <strong>Người báo cáo:</strong> <c:out value="${r.reporter_name}"/> · <c:out value="${r.report_created}"/></p><c:if test="${not empty r.description}"><p><c:out value="${r.description}"/></p></c:if>
  <form method="post" action="${pageContext.request.contextPath}/admin/review-reports" class="fr-moderation-form"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="reviewId" value="${r.review_id}"><label class="form-label">Ghi chú xử lý</label><input class="form-control mb-2" name="note" maxlength="500" placeholder="Ghi chú nội bộ (không bắt buộc)"><div class="d-flex flex-wrap gap-2"><button class="button" name="decision" value="KEEP">Giữ đánh giá</button><button class="btn btn-outline-warning" name="decision" value="HIDE">Ẩn đánh giá</button><button class="btn btn-outline-danger" name="decision" value="REMOVE">Gỡ đánh giá</button></div></form>
 </article></c:forEach>
</section>
<%@ include file="/WEB-INF/views/footer.jsp" %>
