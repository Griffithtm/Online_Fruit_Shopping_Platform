<%@ page pageEncoding="UTF-8" %>
    </main>
    <footer>
        <div class="container">
            <div class="row g-5">
                <div class="col-lg-4">
                    <div class="fnm">Fresh<span>ora</span>
                    </div>
                    <p class="fdesc">Discover fresh fruit from independent sellers. Compare offers, explore stores, and find your everyday favorites.</p><a class="btn-red" href="${pageContext.request.contextPath}/products"><i class="fas fa-shopping-bag"></i>Explore Products</a>
                </div>
                <div class="col-sm-6 col-lg-2">
                    <div class="ftit">Discover</div>
                    <ul class="flinks ps-0">
                        <li><a href="${pageContext.request.contextPath}/home"><i class="fas fa-chevron-right"></i>Home</a>
                        </li>
                        <li><a href="${pageContext.request.contextPath}/products"><i class="fas fa-chevron-right"></i>Products</a>
                        </li>
                        <li><a href="${pageContext.request.contextPath}/shops"><i class="fas fa-chevron-right"></i>Stores</a>
                        </li>
                        <li><a href="${pageContext.request.contextPath}/compare"><i class="fas fa-chevron-right"></i>Compare prices</a>
                        </li>
                    </ul>
                </div>
                <div class="col-sm-6 col-lg-2">
                    <div class="ftit">Your Account</div>
                    <ul class="flinks ps-0">
                        <li><a href="${pageContext.request.contextPath}/login"><i class="fas fa-chevron-right"></i>Log in</a>
                        </li>
                        <li><a href="${pageContext.request.contextPath}/register/customer"><i class="fas fa-chevron-right"></i>Sign up</a>
                        </li>
                        <li><a href="${pageContext.request.contextPath}/account"><i class="fas fa-chevron-right"></i>My account</a>
                        </li>
                        <li><a href="${pageContext.request.contextPath}/cart"><i class="fas fa-chevron-right"></i>My cart</a>
                        </li>
                    </ul>
                </div>
                <div class="col-lg-4">
                    <div class="ftit">Sell With Freshora</div>
                    <div class="fci">
                        <div class="fciico"><i class="fas fa-store"></i>
                        </div>
                        <div class="fciinfo"><strong>Your store, your selection</strong>Register a Seller account to manage your listings and stock.</div>
                    </div><a class="btn-red" href="${pageContext.request.contextPath}/register/seller">Become a Seller <i class="fas fa-arrow-right"></i></a>
                </div>
            </div>
        </div>
        <div class="fbot">
            <div class="container">
                <p>Freshora — SWP391 Group 1. Template design by <a href="https://bestwpware.com/">Bestwpware</a>, distributed by <a href="https://themewagon.com/">ThemeWagon</a>.</p>
            </div>
        </div>
    </footer>
    <button id="btt" type="button" aria-label="Back to top"><i class="fas fa-chevron-up"></i>
    </button>
    <script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/assets/js/freshora-ui.js"></script>
    </body>

    </html>