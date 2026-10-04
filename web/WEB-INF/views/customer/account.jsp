<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <h1>My account</h1>
        <div class="two-column">
            <section class="panel">
                <h2>Personal information</h2>
                <form method="post" action="">
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                    <input type="hidden" name="action" value="profile">
                    <label>Full name
                        <input class="form-control" name="name" type="text" value="${fn:escapeXml(profile.full_name)}" required>
                    </label>
                    <label>Phone
                        <input class="form-control" name="phone" type="tel" value="${fn:escapeXml(profile.phone)}" required>
                    </label>
                    <button class="button">Save profile</button>
                </form>
            </section>
            <section class="panel">
                <h2>Change password</h2>
                <form method="post" action="">
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                    <input type="hidden" name="action" value="password">
                    <label>Current password
                        <input class="form-control" name="old" type="password" value="" required>
                    </label>
                    <label>New password
                        <input class="form-control" name="password" type="password" value="" required>
                    </label>
                    <label>Confirm
                        <input class="form-control" name="confirm" type="password" value="" required>
                    </label>
                    <button class="button">Change password</button>
                </form>
            </section>
        </div>
        <h2>Address book</h2>
        <div class="productgrid">
            <c:forEach items="${addresses}" var="a">
                <section class="panel">
                    <form method="post" action="">
                        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                        <input type="hidden" name="action" value="saveAddress">
                        <input type="hidden" name="id" value="${a.id}">
                        <label>Recipient
                            <input class="form-control" name="name" type="text" value="${fn:escapeXml(a.recipient_name)}" required>
                        </label>
                        <label>Phone
                            <input class="form-control" name="phone" type="tel" value="${fn:escapeXml(a.phone)}" required>
                        </label>
                        <label>Address
                            <input class="form-control" name="line" type="text" value="${fn:escapeXml(a.address_line)}" required>
                        </label>
                        <label>Ward / Commune
                            <input class="form-control" name="ward" type="text" value="${fn:escapeXml(a.ward)}" required>
                        </label>
                        <label>District
                            <input class="form-control" name="district" type="text" value="${fn:escapeXml(a.district)}" required>
                        </label>
                        <label>Province / City
                            <input class="form-control" name="city" type="text" value="${fn:escapeXml(a.city)}" required>
                        </label>
                        <label>
                            <input type="checkbox" name="primary" ${a.is_default ? 'checked' : ''}> Default</label>
                        <button class="button">Update</button>
                    </form>
                    <form method="post" action="">
                        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                        <input type="hidden" name="action" value="deleteAddress">
                        <input type="hidden" name="id" value="${a.id}">
                        <button class="button">Delete address</button>
                    </form>
                </section>
            </c:forEach>
            <section class="panel">
                <h3>Add address</h3>
                <form method="post" action="">
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                    <input type="hidden" name="action" value="saveAddress">
                    <label>Recipient
                        <input class="form-control" name="name" type="text" value="" required>
                    </label>
                    <label>Phone
                        <input class="form-control" name="phone" type="text" value="" required>
                    </label>
                    <label>Address
                        <input class="form-control" name="line" type="text" value="" required>
                    </label>
                    <label>Ward / Commune
                        <input class="form-control" name="ward" type="text" value="" required>
                    </label>
                    <label>District
                        <input class="form-control" name="district" type="text" value="" required>
                    </label>
                    <label>Province / City
                        <input class="form-control" name="city" type="text" value="" required>
                    </label>
                    <label>
                        <input type="checkbox" name="primary"> Default</label>
                    <button class="button">Add address</button>
                </form>
            </section>
        </div>
        <%@ include file="/WEB-INF/views/footer.jsp" %>