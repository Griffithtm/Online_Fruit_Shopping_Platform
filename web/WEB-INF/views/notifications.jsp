<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/header.jsp" %>

<!-- Freshora FE-012: Trang thong bao; du lieu tai bang AJAX, khong can JSP model. -->
<div class="fr-notif-page" id="fr-notif-page">
    <div class="fr-notif-breadcrumb">
        <a href="${pageContext.request.contextPath}/home">Trang chủ</a>
        <i class="fas fa-chevron-right" aria-hidden="true"></i>
        <span>Thông báo</span>
    </div>

    <div class="fr-notif-page-heading">
        <div class="fr-notif-heading-left">
            <span class="fr-notif-eyebrow"><i class="fas fa-leaf" aria-hidden="true"></i> FRESHORA UPDATES</span>
            <h1>Thông báo <span>của bạn</span></h1>
            <p>Cập nhật mới nhất về đơn hàng, thanh toán, cửa hàng và tài khoản của bạn.</p>
        </div>
        <div class="fr-notif-heading-ornament" aria-hidden="true">
            <i class="far fa-bell"></i>
            <span class="fr-notif-leaf"><i class="fas fa-leaf"></i></span>
        </div>
    </div>

    <div class="fr-notif-page-layout">
        <aside class="fr-notif-page-sidebar" aria-label="Bộ lọc thông báo">
            <div class="fr-notif-side-title">Danh mục thông báo</div>
            <button type="button" class="fr-notif-filter active" data-filter="all" aria-pressed="true">
                <span><i class="fas fa-layer-group" aria-hidden="true"></i> Tất cả thông báo</span>
                <span class="fr-notif-side-pill" id="fr-notif-total">Đã lưu</span>
            </button>
            <button type="button" class="fr-notif-filter" data-filter="unread" aria-pressed="false">
                <span><i class="far fa-envelope" aria-hidden="true"></i> Chưa đọc</span>
                <span class="fr-notif-side-pill" id="fr-notif-unread">0</span>
            </button>
            <div class="fr-notif-side-note">
                <i class="fas fa-shield-alt" aria-hidden="true"></i>
                <span>Chỉ bạn mới có thể xem và quản lý thông báo của tài khoản này.</span>
            </div>
        </aside>

        <div class="fr-notif-page-main">
            <div class="fr-notif-list-toolbar">
                <div>
                    <h2 id="fr-notif-list-title">Tất cả thông báo</h2>
                    <p id="fr-notif-list-caption">Thông báo được sắp xếp từ mới đến cũ.</p>
                </div>
                <button type="button" id="fr-notif-read-all" class="fr-notif-read-all" disabled>
                    <i class="fas fa-check-double" aria-hidden="true"></i>
                    Đánh dấu tất cả đã đọc
                </button>
            </div>

            <div class="fr-notif-feedback" id="fr-notif-feedback" role="status" hidden></div>
            <div class="fr-notif-page-items" id="fr-notif-page-items" aria-live="polite" aria-busy="true">
                <div class="fr-notif-loader"><i class="fas fa-spinner fa-spin" aria-hidden="true"></i> Đang tải thông báo...</div>
            </div>
            <div class="fr-notif-pagination" id="fr-notif-pagination" hidden>
                <button type="button" id="fr-notif-prev" class="fr-notif-page-btn">
                    <i class="fas fa-chevron-left" aria-hidden="true"></i> Trang trước
                </button>
                <span id="fr-notif-page-number">Trang 1</span>
                <button type="button" id="fr-notif-next" class="fr-notif-page-btn">
                    Trang sau <i class="fas fa-chevron-right" aria-hidden="true"></i>
                </button>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/footer.jsp" %>
