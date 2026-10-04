<%@ page pageEncoding="UTF-8" %>
    <%@ page contentType="text/html;charset=UTF-8" %>
        <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
            <%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
                <%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
                    <!DOCTYPE html>
                    <html lang="en">

                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width,initial-scale=1">
                        <title>Freshora — Fresh fruit every day</title>
                        <link href="https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;700;900&family=Poppins:wght@300;400;500;600;700&family=Dancing+Script:wght@700&display=swap" rel="stylesheet">
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
                                    <div class="top-contact d-flex flex-wrap"><span><i class="fas fa-leaf"></i>Fresh fruit marketplace</span><span><i class="fas fa-store"></i>Independent sellers</span>
                                    </div><a class="ttag" href="${pageContext.request.contextPath}/register/seller"><i class="fas fa-seedling me-1"></i>Grow with Freshora</a>
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
                                            <div class="bsub">Fresh Fruit Marketplace</div>
                                        </div>
                                    </div>
                                </a>
                                <button class="navbar-toggler border-0" type="button" data-bs-toggle="collapse" data-bs-target="#navmenu" aria-controls="navmenu" aria-expanded="false" aria-label="Toggle navigation"><i class="fas fa-bars"></i>
                                </button>
                                <div class="collapse navbar-collapse" id="navmenu">
                                    <ul class="navbar-nav mx-auto">
                                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/home">Home</a>
                                        </li>
                                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/products">Products</a>
                                        </li>
                                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/shops">Stores</a>
                                        </li>
                                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/compare">Compare</a>
                                        </li>
                                        <c:choose>
                                            <c:when test="${empty sessionScope.user}">
                                                <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/login">Log in</a>
                                                </li>
                                                <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/register/seller">Become a seller</a>
                                                </li>
                                            </c:when>
                                            <c:otherwise>
                                                <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/account">My account</a>
                                                </li>
                                                <c:if test="${sessionScope.user.role == 'SELLER'}">
                                                    <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/seller">Manage store</a>
                                                    </li>
                                                </c:if>
                                                <c:if test="${sessionScope.user.role == 'ADMIN'}">
                                                    <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/admin">Admin</a>
                                                    </li>
                                                </c:if>
                                                <li class="nav-item">
                                                    <form method="post" action="${pageContext.request.contextPath}/logout">
                                                        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                                                        <button class="nav-link logout-link" type="submit">Log out</button>
                                                    </form>
                                                </li>
                                            </c:otherwise>
                                        </c:choose>
                                    </ul>
                                    <div class="d-flex align-items-center gap-1">
                                        <button id="navSearchBtn" type="button" aria-label="Search products"><i class="fas fa-search"></i>
                                        </button>
                                        <c:choose>
                                            <c:when test="${empty sessionScope.user}"><a class="nav-link nav-cta" href="${pageContext.request.contextPath}/register/customer"><i class="fas fa-user-plus me-1"></i>Sign up</a>
                                            </c:when>
                                            <c:when test="${sessionScope.user.role == 'CUSTOMER' || sessionScope.user.role == 'SELLER'}"><a class="nav-link nav-cta" href="${pageContext.request.contextPath}/cart"><i class="fas fa-shopping-bag me-1"></i>My cart</a>
                                            </c:when>
                                        </c:choose>
                                    </div>
                                </div>
                            </div>
                        </nav>
                        <div id="searchOv" role="dialog" aria-modal="true" aria-label="Search products" hidden>
                            <button class="sovclose" id="searchClose" aria-label="Close search"><i class="fas fa-times"></i>
                            </button>
                            <div class="sovbox">
                                <h4>What fruit are you looking for?</h4>
                                <form action="${pageContext.request.contextPath}/products" method="get" class="sovinput">
                                    <input id="searchInput" name="q" placeholder="Search oranges, apples, mangoes..." required>
                                    <button type="submit" aria-label="Search"><i class="fas fa-search"></i>
                                    </button>
                                </form>
                            </div>
                        </div>
                        <main class="${homePage ? 'home-page' : 'container pagebody'}">
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
                                <div class="alert alert-success">Changes saved successfully.</div>
                            </c:if>