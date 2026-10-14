<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <h1>Tài khoản của tôi</h1>
        <div class="two-column">
            <section class="panel">
                <h2>Thông tin cá nhân</h2>
                <form method="post" action="">
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                    <input type="hidden" name="action" value="profile">
                    <label>Họ tên
                        <input class="form-control" name="name" type="text" value="${fn:escapeXml(profile.fullName)}" required>
                    </label>
                    <label>Điện thoại
                        <input class="form-control" name="phone" type="tel" value="${fn:escapeXml(profile.phone)}" required>
                    </label>
                    <button class="button">Lưu hồ sơ</button>
                </form>
            </section>
            <section class="panel">
                <h2>Đổi mật khẩu</h2>
                <form method="post" action="">
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                    <input type="hidden" name="action" value="password">
                    <label>Mật khẩu hiện tại
                        <input class="form-control" name="old" type="password" value="" required>
                    </label>
                    <label>Mật khẩu mới
                        <input class="form-control" name="password" type="password" value="" required>
                    </label>
                    <label>Xác nhận
                        <input class="form-control" name="confirm" type="password" value="" required>
                    </label>
                    <button class="button">Đổi mật khẩu</button>
                </form>
            </section>
        </div>
        <h2>Sổ địa chỉ</h2>
        <div class="productgrid">
            <c:forEach items="${addresses}" var="a">
                <section class="panel">
                    <form method="post" action="">
                        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                        <input type="hidden" name="action" value="saveAddress">
                        <input type="hidden" name="id" value="${a.id}">
                        <label>Người nhận
                            <input class="form-control" name="name" type="text" value="${fn:escapeXml(a.recipientName)}" required>
                        </label>
                        <label>Điện thoại
                            <input class="form-control" name="phone" type="tel" value="${fn:escapeXml(a.phone)}" required>
                        </label>
                        <label>Địa chỉ
                            <input class="form-control" name="line" type="text" value="${fn:escapeXml(a.addressLine)}" required>
                        </label>
                        <label>Phường/Xã
                            <input class="form-control" name="ward" type="text" value="${fn:escapeXml(a.ward)}" required>
                        </label>
                        <label>Quận/Huyện
                            <input class="form-control" name="district" type="text" value="${fn:escapeXml(a.district)}" required>
                        </label>
                        <label>Tỉnh/Thành phố
                            <input class="form-control" name="city" type="text" value="${fn:escapeXml(a.city)}" required>
                        </label>
                        <label>
                            <input type="checkbox" name="primary" ${a.isDefault ? 'checked' : ''}> Mặc định</label>
                        <button class="button">Cập nhật</button>
                    </form>
                    <form method="post" action="">
                        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                        <input type="hidden" name="action" value="deleteAddress">
                        <input type="hidden" name="id" value="${a.id}">
                        <button class="button">Xóa địa chỉ</button>
                    </form>
                </section>
            </c:forEach>
            <section class="panel">
                <h3>Thêm địa chỉ</h3>
                <form method="post" action="">
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                    <input type="hidden" name="action" value="saveAddress">
                    <label>Người nhận
                        <input class="form-control" name="name" type="text" value="" required>
                    </label>
                    <label>Điện thoại
                        <input class="form-control" name="phone" type="text" value="" required>
                    </label>
                    <label>Địa chỉ
                        <input class="form-control" name="line" type="text" value="" required>
                    </label>
                    <label>Phường/Xã
                        <input class="form-control" name="ward" type="text" value="" required>
                    </label>
                    <label>Quận/Huyện
                        <input class="form-control" name="district" type="text" value="" required>
                    </label>
                    <label>Tỉnh/Thành phố
                        <input class="form-control" name="city" type="text" value="" required>
                    </label>
                    <label>
                        <input type="checkbox" name="primary"> Mặc định</label>
                    <button class="button">Thêm địa chỉ</button>
                </form>
            </section>
        </div>
        <%@ include file="/WEB-INF/views/footer.jsp" %>