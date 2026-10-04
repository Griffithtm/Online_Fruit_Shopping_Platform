<%@ page pageEncoding="UTF-8" %>
    <% request.setAttribute( "homePage", true); %>
        <%@ include file="/WEB-INF/views/header.jsp" %>
            <section id="hero">
                <div class="hs hs1"></div>
                <div class="hs hs2"></div>
                <div class="hbgtxt">FRESH</div>
                <div class="container">
                    <div class="row align-items-center g-5" style="min-height:88vh;">
                        <div class="col-lg-6">
                            <div class="hbadge">
                                <div class="hbi"><i class="fas fa-star"></i>
                                </div>
                                <span>Fresh choices from independent sellers</span>
                            </div>
                            <h1 class="htitle">Delicious <span class="hl">Fresh Fruit</span><br/>for Every Moment</h1>
                            <p class="hdesc">Explore seasonal fruit, discover local stores, and compare offers. Find something fresh for every day, all in one marketplace.</p>
                            <div class="d-flex flex-wrap gap-3 mb-2">
                                <a href="${pageContext.request.contextPath}/products" class="btn-red"><i class="fas fa-utensils"></i>Explore Products</a>
                                <!-- FIX 2: Magnific popup video trigger -->
                                <a href="${pageContext.request.contextPath}/shops" class="btn-play">
                                    <div class="pico"><i class="fas fa-store"></i>
                                    </div><span>Explore Our Stores</span>
                                </a>
                            </div>
                            <div class="hstats d-flex gap-3 flex-wrap mt-4">
                                <div class="hstat"><span class="snum">${fn:length(products)}</span><small>Available offers</small>
                                </div>
                                <div class="sdiv"></div>
                                <div class="hstat"><span class="snum">${fn:length(categories)}</span><small>Fruit categories</small>
                                </div>
                                <div class="sdiv"></div>
                                <div class="hstat"><span class="snum"><i class="fas fa-leaf"></i></span><small>Fresh possibilities</small>
                                </div>
                            </div>
                        </div>
                        <div class="col-lg-6">
                            <div style="position:relative;text-align:center;">
                                <div class="hcircle">
                                    <img src="${pageContext.request.contextPath}/assets/img/banner-img.jpg" alt="Sarab food banner" />
                                </div>
                                <div class="fcard fc1">
                                    <div class="fcoi r"><i class="fas fa-fire"></i>
                                    </div>
                                    <div><span class="fcnum">Discover</span><span class="fcsm">Seasonal selections</span>
                                    </div>
                                </div>
                                <div class="fcard fc2">
                                    <div class="fcoi y"><i class="fas fa-star"></i>
                                    </div>
                                    <div><span class="fcnum">Compare</span><span class="fcsm">Offers from sellers</span>
                                    </div>
                                </div>
                                <div class="fcard fc3">
                                    <div class="fcoi g"><i class="fas fa-clock"></i>
                                    </div>
                                    <div><span class="fcnum">Your choice</span><span class="fcsm">Shop your way</span>
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
                        <div class="mqitem"><i class="fas fa-circle"></i>Seasonal Fruit</div>
                        <div class="mqitem"><i class="fas fa-circle"></i>Independent Stores</div>
                        <div class="mqitem"><i class="fas fa-circle"></i>Fresh Choices</div>
                        <div class="mqitem"><i class="fas fa-circle"></i>Compare Offers</div>
                    </c:forEach>
                </div>
            </div>
            <section id="category">
                <div class="container">
                    <div class="text-center mb-5"><span class="slbl">What We Offer</span>
                        <h2 class="stitle">Browse by <span>Category</span></h2>
                        <div class="sline"></div>
                        <p class="sdesc mx-auto">Discover the right fruit for your table.</p>
                    </div>
                    <div class="row g-3 justify-content-center">
                        <div class="col-6 col-md-3 col-lg-2">
                            <a class="catcard active d-block" href="${pageContext.request.contextPath}/products">
                                <div class="category-icon"><i class="fas fa-apple-alt"></i>
                                </div>
                                <div class="catnm">All Products</div>
                                <div class="catct">Explore the marketplace</div>
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
                                    <div class="catct">Discover more</div>
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
                                <div class="aexp"><span class="anum"><i class="fas fa-leaf"></i></span><small>Fresh choices<br>Every day</small>
                                </div>
                                <div class="amain"><img src="${pageContext.request.contextPath}/assets/img/about1.jpg" alt="Sarab dining atmosphere">
                                </div>
                                <div class="asm"><img src="${pageContext.request.contextPath}/assets/img/about2.jpg" alt="Sarab food presentation">
                                </div>
                            </div>
                        </div>
                        <div class="col-lg-7"><span class="slbl">Our Marketplace</span>
                            <h2 class="stitle text-start">A Fresh Way to Shop<br>From <span>Independent Sellers</span></h2>
                            <div class="sline lft"></div>
                            <p class="sdesc mb-4">Freshora brings fruit stores together so you can explore products, compare pack sizes and prices, and choose the offers that suit you.</p>
                            <div class="fti">
                                <div class="ftico r"><i class="fas fa-leaf"></i>
                                </div>
                                <div>
                                    <h6>Discover your favorites</h6>
                                    <p>Browse fruit by category, origin, and available variants.</p>
                                </div>
                            </div>
                            <div class="fti">
                                <div class="ftico y"><i class="fas fa-store"></i>
                                </div>
                                <div>
                                    <h6>Explore independent stores</h6>
                                    <p>Visit each store and compare its offers.</p>
                                </div>
                            </div>
                            <div class="fti">
                                <div class="ftico g"><i class="fas fa-shopping-bag"></i>
                                </div>
                                <div>
                                    <h6>Build your basket</h6>
                                    <p>Keep your choices together in a cart grouped by seller.</p>
                                </div>
                            </div><a href="${pageContext.request.contextPath}/products" class="btn-red mt-3"><i class="fas fa-book-open"></i>View All Products</a>
                        </div>
                    </div>
                </div>
            </section>
            <section id="menu">
                <div class="container">
                    <div class="text-center mb-5"><span class="slbl">Fresh Finds</span>
                        <h2 class="stitle">Explore Our <span>Products</span></h2>
                        <div class="sline"></div>
                    </div>
                    <div class="text-center mb-4"><a class="filtbtn active" href="${pageContext.request.contextPath}/products">All Products</a>
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
                    <div class="nlw text-center"><span class="slbl" style="color:#fff">Grow With Us</span>
                        <h2 class="mb-3" style="color:#fff">Bring Your Store to <span style="color:var(--secondary)">Freshora</span></h2>
                        <p class="mb-4" style="color:#fff">Create your Seller account and start preparing your fruit listings.</p><a href="${pageContext.request.contextPath}/register/seller" class="btn-red" style="background:#fff;color:var(--primary)">Open Your Store <i class="fas fa-arrow-right"></i></a>
                    </div>
                </div>
            </section>
            <%@ include file="/WEB-INF/views/footer.jsp" %>