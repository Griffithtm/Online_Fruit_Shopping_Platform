<%@ page pageEncoding="UTF-8" %>
    <% request.setAttribute( "homePage", true); %>
        <%@ include file="/WEB-INF/views/header.jsp" %>
            <section id="hero">
                <div class="hs hs1"></div>
                <div class="hs hs2"></div>
                <div class="hbgtxt">TƯƠI NGON</div>
                <div class="container">
                    <div class="row align-items-center g-5" style="min-height:88vh;">
                        <div class="col-lg-6">
                            <div class="hbadge">
                                <div class="hbi"><i class="fas fa-star"></i>
                                </div>
                                <span>Trái cây tươi từ các nhà bán độc lập</span>
                            </div>
                            <h1 class="htitle">Thơm ngon <span class="hl">Trái cây tươi</span><br/>cho mọi khoảnh khắc</h1>
                            <p class="hdesc">Khám phá trái cây theo mùa, tìm cửa hàng địa phương và so sánh sản phẩm. Chọn trái cây tươi mỗi ngày ngay trên một sàn mua sắm.</p>
                            <div class="d-flex flex-wrap gap-3 mb-2">
                                <a href="${pageContext.request.contextPath}/products" class="btn-red"><i class="fas fa-utensils"></i>Khám phá sản phẩm</a>
                                <!-- FIX 2: Magnific popup video trigger -->
                                <a href="${pageContext.request.contextPath}/shops" class="btn-play">
                                    <div class="pico"><i class="fas fa-store"></i>
                                    </div><span>Khám phá các cửa hàng</span>
                                </a>
                            </div>
                            <div class="hstats d-flex gap-3 flex-wrap mt-4">
                                <div class="hstat"><span class="snum">${fn:length(products)}</span><small>Các lựa chọn đang bán</small>
                                </div>
                                <div class="sdiv"></div>
                                <div class="hstat"><span class="snum">${fn:length(categories)}</span><small>Danh mục trái cây</small>
                                </div>
                                <div class="sdiv"></div>
                                <div class="hstat"><span class="snum"><i class="fas fa-leaf"></i></span><small>Trải nghiệm mới</small>
                                </div>
                            </div>
                        </div>
                        <div class="col-lg-6">
                            <div style="position:relative;text-align:center;">
                                <div class="hcircle">
                                    <img src="${pageContext.request.contextPath}/assets/img/fruits/fruit-basket.jpg" alt="Giỏ trái cây tươi theo mùa" />
                                </div>
                                <div class="fcard fc1">
                                    <div class="fcoi r"><i class="fas fa-fire"></i>
                                    </div>
                                    <div><span class="fcnum">Khám phá</span><span class="fcsm">Lựa chọn theo mùa</span>
                                    </div>
                                </div>
                                <div class="fcard fc2">
                                    <div class="fcoi y"><i class="fas fa-star"></i>
                                    </div>
                                    <div><span class="fcnum">So sánh</span><span class="fcsm">Sản phẩm từ các nhà bán</span>
                                    </div>
                                </div>
                                <div class="fcard fc3">
                                    <div class="fcoi g"><i class="fas fa-clock"></i>
                                    </div>
                                    <div><span class="fcnum">Lựa chọn của bạn</span><span class="fcsm">Mua sắm theo cách của bạn</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            <div class="mqsec">
                <div class="mqtrack">
                    <c:forEach begin="1" end="3">
                        <div class="mqitem"><i class="fas fa-circle"></i>Trái cây theo mùa</div>
                        <div class="mqitem"><i class="fas fa-circle"></i>Cửa hàng độc lập</div>
                        <div class="mqitem"><i class="fas fa-circle"></i>Lựa chọn tươi ngon</div>
                        <div class="mqitem"><i class="fas fa-circle"></i>So sánh sản phẩm</div>
                    </c:forEach>
                </div>
            </div>
            <section id="category">
                <div class="container">
                    <div class="text-center mb-5"><span class="slbl">Sản phẩm và dịch vụ</span>
                        <h2 class="stitle">Khám phá theo <span>Danh mục</span></h2>
                        <div class="sline"></div>
                        <p class="sdesc mx-auto">Chọn trái cây phù hợp cho bữa ăn gia đình.</p>
                    </div>
                    <div class="row g-3 justify-content-center">
                        <div class="col-6 col-md-3 col-lg-2">
                            <a class="catcard active d-block" href="${pageContext.request.contextPath}/products">
                                <div class="category-icon"><i class="fas fa-apple-alt"></i>
                                </div>
                                <div class="catnm">Tất cả sản phẩm</div>
                                <div class="catct">Khám phá sàn mua sắm</div>
                            </a>
                        </div>
                        <c:forEach items="${categories}" var="category">
                            <div class="col-6 col-md-3 col-lg-2">
                                <a class="catcard d-block" href="${pageContext.request.contextPath}/products?category=${category.id}">
                                    <div class="category-icon"><i class="fas fa-leaf"></i>
                                    </div>
                                    <div class="catnm">
                                        <c:out value="${category.name}" />
                                    </div>
                                    <div class="catct">Khám phá thêm</div>
                                </a>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </section>
            <section id="about">
                <div class="container">
                    <div class="row align-items-center g-5">
                        <div class="col-lg-5">
                            <div class="astack">
                                <div class="aexp"><span class="anum"><i class="fas fa-leaf"></i></span><small>Lựa chọn tươi ngon<br>Mỗi ngày</small>
                                </div>
                                <div class="amain"><img src="${pageContext.request.contextPath}/assets/img/fruits/fruit-orchard.jpg" alt="Táo tươi trên cây trong vườn">
                                </div>
                                <div class="asm"><img src="${pageContext.request.contextPath}/assets/img/fruits/fresh-fruit.jpg" alt="Những loại trái cây tươi ngon">
                                </div>
                            </div>
                        </div>
                        <div class="col-lg-7"><span class="slbl">Sàn mua sắm của chúng tôi</span>
                            <h2 class="stitle text-start">Mua sắm trái cây theo cách mới<br>Từ <span>Nhà bán độc lập</span></h2>
                            <div class="sline lft"></div>
                            <p class="sdesc mb-4">Freshora kết nối các cửa hàng trái cây để bạn khám phá sản phẩm, so sánh quy cách và giá bán, rồi chọn sản phẩm phù hợp.</p>
                            <div class="fti">
                                <div class="ftico r"><i class="fas fa-leaf"></i>
                                </div>
                                <div>
                                    <h6>Tìm trái cây yêu thích</h6>
                                    <p>Khám phá trái cây theo danh mục, xuất xứ và quy cách đang bán.</p>
                                </div>
                            </div>
                            <div class="fti">
                                <div class="ftico y"><i class="fas fa-store"></i>
                                </div>
                                <div>
                                    <h6>Khám phá cửa hàng độc lập</h6>
                                    <p>Ghé từng cửa hàng và so sánh sản phẩm.</p>
                                </div>
                            </div>
                            <div class="fti">
                                <div class="ftico g"><i class="fas fa-shopping-bag"></i>
                                </div>
                                <div>
                                    <h6>Chọn trái cây cho giỏ hàng</h6>
                                    <p>Quản lý các sản phẩm đã chọn trong giỏ hàng theo từng cửa hàng.</p>
                                </div>
                            </div><a href="${pageContext.request.contextPath}/products" class="btn-red mt-3"><i class="fas fa-book-open"></i>Xem tất cả sản phẩm</a>
                        </div>
                    </div>
                </div>
            </section>
            <section id="menu">
                <div class="container">
                    <div class="text-center mb-5"><span class="slbl">Trái cây mới</span>
                        <h2 class="stitle">Khám phá <span>Sản phẩm</span></h2>
                        <div class="sline"></div>
                    </div>
                    <div class="text-center mb-4"><a class="filtbtn active" href="${pageContext.request.contextPath}/products">Tất cả sản phẩm</a>
                        <c:forEach items="${categories}" var="category">
                            <a class="filtbtn" href="${pageContext.request.contextPath}/products?category=${category.id}">
                                <c:out value="${category.name}" />
                            </a>
                        </c:forEach>
                    </div>
                    <%@ include file="/WEB-INF/views/cards.jsp" %>
                </div>
            </section>
            <section id="newsletter">
                <div class="nlbg"></div>
                <div class="container">
                    <div class="nlw text-center"><span class="slbl" style="color:#fff">Đồng hành cùng chúng tôi</span>
                        <h2 class="mb-3" style="color:#fff">Đưa cửa hàng của bạn đến <span style="color:var(--secondary)">Freshora</span></h2>
                        <p class="mb-4" style="color:#fff">Đăng ký tài khoản người bán và bắt đầu đăng sản phẩm trái cây của bạn.</p><a href="${pageContext.request.contextPath}/register/seller" class="btn-red" style="background:#fff;color:var(--primary)">Mở cửa hàng của bạn <i class="fas fa-arrow-right"></i></a>
                    </div>
                </div>
            </section>
            <%@ include file="/WEB-INF/views/footer.jsp" %>