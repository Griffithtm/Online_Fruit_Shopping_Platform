<%@ page pageEncoding="UTF-8" %>
    <%@ page contentType="text/html;charset=UTF-8" %>
        <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
            <%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
                <%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
                    <%@ taglib prefix="vi" uri="/WEB-INF/vietnamese.tld" %>
<fmt:setLocale value="vi_VN" scope="session"/>
<!DOCTYPE html>
                    <html lang="vi">

                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width,initial-scale=1">
                        <title>Freshora — Trái cây tươi mỗi ngày</title>
                        <link href="https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;700;900&family=Be+Vietnam+Pro:wght@300;400;500;600;700&family=Dancing+Script:wght@700&display=swap" rel="stylesheet">
                        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/bootstrap.min.css">
                        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/aos.css">
                        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/all.min.css">
                        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
                        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/freshora.css">
                    </head>

                    <body>
                        <div id="topbar">
                            <div class="container">
                                <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
                                    <div class="top-contact d-flex flex-wrap"><span><i class="fas fa-leaf"></i>Sàn trái cây tươi</span><span><i class="fas fa-store"></i>Nhà bán độc lập</span>
                                    </div><a class="ttag" href="${pageContext.request.contextPath}/register/seller"><i class="fas fa-seedling me-1"></i>Kinh doanh cùng Freshora</a>
                                </div>
                            </div>
                        </div>
                        <nav class="navbar navbar-expand-lg" id="nav">
                            <div class="container">
                                <a class="navbar-brand" href="${pageContext.request.contextPath}/home">
                                    <div class="blogo">
                                        <div class="bico"><i class="fas fa-apple-alt"></i>
                                        </div>
                                        <div>
                                            <div class="bname">Fresh<span>ora</span>
                                            </div>
                                            <div class="bsub">Sàn trái cây tươi</div>
                                        </div>
                                    </div>
                                </a>
                                <button class="navbar-toggler border-0" type="button" data-bs-toggle="collapse" data-bs-target="#navmenu" aria-controls="navmenu" aria-expanded="false" aria-label="Mở hoặc đóng trình đơn"><i class="fas fa-bars"></i>
                                </button>
                                <div class="collapse navbar-collapse" id="navmenu">
                                    <ul class="navbar-nav mx-auto">
                                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/home">Trang chủ</a>
                                        </li>
                                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/products">Sản phẩm</a>
                                        </li>
                                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/shops">Cửa hàng</a>
                                        </li>
                                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/compare">So sánh</a>
                                        </li>
                                        <c:choose>
                                            <c:when test="${empty sessionScope.user}">
                                                <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/login">Đăng nhập</a>
                                                </li>
                                                <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/register/seller">Đăng ký bán hàng</a>
                                                </li>
                                            </c:when>
                                            <c:otherwise>
<li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/account">Tài khoản của tôi</a>
                                                </li>
                                                <c:if test="${sessionScope.user.role == 'SELLER'}">
                                                    <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/seller">Quản lý cửa hàng</a>
                                                    </li>
                                                </c:if>
                                                <c:if test="${sessionScope.user.role == 'ADMIN'}">
                                                    <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/admin">Quản trị</a>
                                                    </li>
                                                </c:if>
                                                <li class="nav-item">
                                                    <form method="post" action="${pageContext.request.contextPath}/logout">
                                                        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                                                        <button class="nav-link logout-link" type="submit">Đăng xuất</button>
                                                    </form>
                                                </li>
                                            </c:otherwise>
                                        </c:choose>
                                    </ul>
                                    <div class="d-flex align-items-center gap-1">
                                        <button id="navSearchBtn" type="button" aria-label="Tìm sản phẩm"><i class="fas fa-search"></i>
                                        </button>
                                        <c:choose>
                                            <c:when test="${empty sessionScope.user}"><a class="nav-link nav-cta" href="${pageContext.request.contextPath}/register/customer"><i class="fas fa-user-plus me-1"></i>Đăng ký</a>
                                            </c:when>
                                            <c:when test="${sessionScope.user.role == 'CUSTOMER' || sessionScope.user.role == 'SELLER'}"><a class="nav-link nav-cta" href="${pageContext.request.contextPath}/cart"><i class="fas fa-shopping-bag me-1"></i>Giỏ hàng của tôi</a>
                                            </c:when>
                                        </c:choose>
                                    </div>
                                </div>
                            </div>
                        </nav>
                        <div id="searchOv" role="dialog" aria-modal="true" aria-label="Tìm sản phẩm" hidden>
                            <button class="sovclose" id="searchClose" aria-label="Đóng tìm kiếm"><i class="fas fa-times"></i>
                            </button>
                            <div class="sovbox">
                                <h4>Bạn đang tìm loại trái cây nào?</h4>
                                <form action="${pageContext.request.contextPath}/products" method="get" class="sovinput">
                                    <input id="searchInput" name="q" placeholder="Tìm cam, táo, xoài..." required>
                                    <button type="submit" aria-label="Tìm kiếm"><i class="fas fa-search"></i>
                                    </button>
                                </form>
                            </div>
                        </div>
                        <main class="${homePage ? 'home-page' : 'container pagebody'}">
<c:if test="${not homePage && not empty sessionScope.user}"><nav aria-label="Khu vực làm việc" class="d-flex flex-wrap gap-3 mb-4">
<c:if test="${sessionScope.user.role == 'CUSTOMER' || sessionScope.user.role == 'SELLER'}"><a href="${pageContext.request.contextPath}/orders">Đơn hàng của tôi</a><a href="${pageContext.request.contextPath}/wallet">Điểm thưởng</a><a href="${pageContext.request.contextPath}/refunds">Yêu cầu hoàn tiền của tôi</a></c:if>
<c:if test="${sessionScope.user.role == 'SELLER'}"><a href="${pageContext.request.contextPath}/seller/orders">Đơn hàng cửa hàng</a><a href="${pageContext.request.contextPath}/seller/promotions">Chiến dịch khuyến mãi</a></c:if>
<c:if test="${sessionScope.user.role == 'ADMIN'}"><a href="${pageContext.request.contextPath}/admin/orders">Xử lý đơn hàng</a><a href="${pageContext.request.contextPath}/admin/promotions">Mã giảm giá</a><a href="${pageContext.request.contextPath}/admin/refunds">Duyệt hoàn tiền</a></c:if>
<c:if test="${sessionScope.user.role == 'DELIVERY_STAFF'}"><a href="${pageContext.request.contextPath}/delivery">Giao hàng</a></c:if>
</nav></c:if>

                            <c:if test="${not empty error}">
                                <div role="alert" class="alert alert-danger">
                                    <c:out value="${error}" />
                                </div>
                            </c:if>
                            <c:if test="${not empty message}">
                                <div class="alert alert-info">
                                    <c:out value="${message}" />
                                </div>
                            </c:if>
                            <c:if test="${param.saved == '1' || param.success == '1'}">
                                <div class="alert alert-success">Thao tác thành công.</div>
                            </c:if>
