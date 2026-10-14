<%@ page pageEncoding="UTF-8" %>
    </main>
    <footer>
        <div class="container">
            <div class="row g-5">
                <div class="col-lg-4">
                    <div class="fnm">Fresh<span>ora</span>
                    </div>
                    <p class="fdesc">Khám phá trái cây tươi từ các nhà bán độc lập. So sánh sản phẩm, tìm cửa hàng và chọn trái cây yêu thích mỗi ngày.</p><a class="btn-red" href="${pageContext.request.contextPath}/products"><i class="fas fa-shopping-bag"></i>Khám phá sản phẩm</a>
                </div>
                <div class="col-sm-6 col-lg-2">
                    <div class="ftit">Khám phá</div>
                    <ul class="flinks ps-0">
                        <li><a href="${pageContext.request.contextPath}/home"><i class="fas fa-chevron-right"></i>Trang chủ</a>
                        </li>
                        <li><a href="${pageContext.request.contextPath}/products"><i class="fas fa-chevron-right"></i>Sản phẩm</a>
                        </li>
                        <li><a href="${pageContext.request.contextPath}/shops"><i class="fas fa-chevron-right"></i>Cửa hàng</a>
                        </li>
                        <li><a href="${pageContext.request.contextPath}/compare"><i class="fas fa-chevron-right"></i>So sánh giá</a>
                        </li>
                    </ul>
                </div>
                <div class="col-sm-6 col-lg-2">
                    <div class="ftit">Tài khoản của bạn</div>
                    <ul class="flinks ps-0">
                        <li><a href="${pageContext.request.contextPath}/login"><i class="fas fa-chevron-right"></i>Đăng nhập</a>
                        </li>
                        <li><a href="${pageContext.request.contextPath}/register/customer"><i class="fas fa-chevron-right"></i>Đăng ký</a>
                        </li>
                        <li><a href="${pageContext.request.contextPath}/account"><i class="fas fa-chevron-right"></i>Tài khoản của tôi</a>
                        </li>
                        <li><a href="${pageContext.request.contextPath}/cart"><i class="fas fa-chevron-right"></i>Giỏ hàng của tôi</a>
                        </li>
                    </ul>
                </div>
                <div class="col-lg-4">
                    <div class="ftit">Bán hàng cùng Freshora</div>
                    <div class="fci">
                        <div class="fciico"><i class="fas fa-store"></i>
                        </div>
                        <div class="fciinfo"><strong>Cửa hàng của bạn, sản phẩm của bạn</strong>Đăng ký tài khoản người bán để quản lý tin đăng và tồn kho.</div>
                    </div><a class="btn-red" href="${pageContext.request.contextPath}/register/seller">Đăng ký bán hàng <i class="fas fa-arrow-right"></i></a>
                </div>
            </div>
        </div>
        <div class="fbot">
            <div class="container">
                <p>Freshora — SWP391 Nhóm 1. Thiết kế giao diện bởi <a href="https://bestwpware.com/">Bestwpware</a>, phân phối bởi <a href="https://themewagon.com/">ThemeWagon</a>.</p>
            </div>
        </div>
    </footer>
    <button id="btt" type="button" aria-label="Về đầu trang"><i class="fas fa-chevron-up"></i>
    </button>
    <script src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/assets/js/freshora-ui.js"></script>
    </body>

    </html>
