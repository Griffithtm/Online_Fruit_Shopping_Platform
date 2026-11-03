<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/header.jsp" %>
<div class="container py-4">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <div><h1>Hậu kiểm sản phẩm</h1><p class="text-muted mb-0">Kiểm tra các sản phẩm đã được công khai; đình chỉ sản phẩm vi phạm để ẩn khỏi gian hàng.</p></div>
    </div>
    <c:if test="${not empty error}"><div class="alert alert-danger"><c:out value="${error}"/></div></c:if>
    <c:if test="${param.saved == '1'}"><div class="alert alert-success">Đã cập nhật trạng thái sản phẩm.</div></c:if>

    <form method="get" action="${pageContext.request.contextPath}/admin/listing-reviews" class="panel p-3 mb-3">
        <div class="row g-3 align-items-end">
            <div class="col-md-3"><label class="form-label" for="status">Trạng thái</label>
                <select class="form-select" id="status" name="status">
                    <option value="PENDING_REVIEW" ${filterStatus == 'PENDING_REVIEW' ? 'selected' : ''}>Chờ duyệt lại</option>
                    <option value="PUBLISHED" ${filterStatus == 'PUBLISHED' ? 'selected' : ''}>Đang công khai</option>
                    <option value="SUSPENDED" ${filterStatus == 'SUSPENDED' ? 'selected' : ''}>Đình chỉ</option>
                    <option value="HIDDEN" ${filterStatus == 'HIDDEN' ? 'selected' : ''}>Đang ẩn</option>
                    <option value="DRAFT" ${filterStatus == 'DRAFT' ? 'selected' : ''}>Bản nháp</option>
                    <option value="RETIRED" ${filterStatus == 'RETIRED' ? 'selected' : ''}>Ngừng kinh doanh</option>
                    <option value="ALL" ${filterStatus == 'ALL' ? 'selected' : ''}>Tất cả</option>
                </select>
            </div>
            <div class="col-md-4"><label class="form-label" for="store">Cửa hàng</label>
                <select class="form-select" id="store" name="store"><option value="">Tất cả cửa hàng</option>
                    <c:forEach items="${stores}" var="st"><option value="${st.id}" ${filterStore == st.id.toString() ? 'selected' : ''}><c:out value="${st.storeName}"/></option></c:forEach>
                </select>
            </div>
            <div class="col-md-3"><label class="form-label" for="q">Tên hoặc mã sản phẩm</label><input class="form-control" id="q" name="q" value="<c:out value='${keyword}'/>" placeholder="Nhập tên / ID"></div>
            <div class="col-md-2"><button class="button w-100">Lọc</button></div>
        </div>
    </form>

    <div class="d-flex justify-content-between mb-2"><strong>${total} sản phẩm</strong><span class="text-muted">Trang ${page}/${pages}</span></div>
    <div class="table-responsive panel">
        <table class="table align-middle mb-0">
            <thead><tr><th>Sản phẩm</th><th>Cửa hàng</th><th>Trạng thái</th><th>Cập nhật</th><th>Thao tác</th></tr></thead>
            <tbody>
            <c:forEach items="${listings}" var="l">
                <tr>
                    <td><div class="d-flex align-items-center gap-2">
                        <c:if test="${not empty l.image}"><img src="${pageContext.request.contextPath}${fn:escapeXml(l.image)}" alt="" style="width:64px;height:64px;object-fit:cover;border-radius:8px"></c:if>
                        <div><strong><c:out value="${l.title}"/></strong><div class="small text-muted">Mã #${l.id}</div>
                            <c:if test = "${l.status=='PUBLISHED'}">
                                <a href="${pageContext.request.contextPath}/product?id=${l.id}" target="_blank" rel="noopener">Xem chi tiết trên Freshora ↗</a>
                            </c:if>
                        </div></div></td>
                    <td><c:out value="${l.storeName}"/><div class="small text-muted">Store #${l.storeId}</div></td>
                    <td><span class="badge bg-${l.status == 'PUBLISHED' ? 'success' : l.status == 'SUSPENDED' ? 'danger' : 'secondary'}"><c:out value="${vi:label(l.status)}"/></span>
                        <c:if test="${l.status == 'SUSPENDED' and not empty l.rejectionReason}"><div class="small text-muted mt-1">Lý do: <c:out value="${l.rejectionReason}"/></div></c:if>
                    </td>
                    <td><fmt:formatDate value="${l.updatedAt}" pattern="dd/MM/yyyy HH:mm"/></td>
                    <td style="min-width:210px">
                        <c:choose>
                            <c:when test="${l.status == 'PUBLISHED'}">
                                <form method="post" action="${pageContext.request.contextPath}/admin/listing-reviews" onsubmit="return confirm('Đình chỉ sản phẩm này? Sản phẩm sẽ không còn hiển thị cho khách hàng.');">
                                    <input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="suspend"><input type="hidden" name="id" value="${l.id}">
                                    <input class="form-control form-control-sm mb-1" name="reason" maxlength="500" required placeholder="Lý do đình chỉ">
                                    <button class="btn btn-sm btn-danger">Đình chỉ sản phẩm</button>
                                </form>
                            </c:when>
                            <c:when test="${l.status == 'SUSPENDED'}">
                                <form method="post" action="${pageContext.request.contextPath}/admin/listing-reviews" onsubmit="return confirm('Mở đình chỉ và công khai lại sản phẩm?');">
                                    <input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="restore"><input type="hidden" name="id" value="${l.id}">
                                    <button class="btn btn-sm btn-outline-success">Gỡ đình chỉ</button>
                                </form>
                            </c:when>
                            <c:otherwise><span class="text-muted small">Không có thao tác</span></c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty listings}"><tr><td colspan="5" class="text-center text-muted py-4">Không tìm thấy sản phẩm phù hợp bộ lọc.</td></tr></c:if>
            </tbody>
        </table>
    </div>
    <c:if test="${pages > 1}"><nav class="mt-3"><ul class="pagination">
        <c:forEach begin="1" end="${pages}" var="p"><li class="page-item ${p == page ? 'active' : ''}"><a class="page-link" href="?status=${filterStatus}&store=${filterStore}&q=${fn:escapeXml(keyword)}&page=${p}">${p}</a></li></c:forEach>
    </ul></nav></c:if>
</div>
<%@ include file="/WEB-INF/views/footer.jsp" %>
