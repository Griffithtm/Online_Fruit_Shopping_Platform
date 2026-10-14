<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <h1>My account</h1>
        <div class="two-column">
            <section class="panel">
                <h2>Personal information</h2>
                <c:set var="pf" value="${empty profileForm ? profile : profileForm}" />
                <form method="post" action="" novalidate>
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                    <input type="hidden" name="action" value="profile">
                    <label>Full name
                        <input class="form-control ${not empty fieldErrors.name ? 'is-invalid' : ''}" name="name" type="text" maxlength="100" value="${fn:escapeXml(pf.full_name)}" required aria-describedby="profile-name-error" ${not empty fieldErrors.name ? 'aria-invalid="true"' : ''}>
                    </label>
                    <c:if test="${not empty fieldErrors.name}"><div id="profile-name-error" class="invalid-feedback d-block"><c:out value="${fieldErrors.name}" /></div></c:if>
                    <label>Email
                        <input class="form-control ${not empty fieldErrors.email ? 'is-invalid' : ''}" name="email" type="email" maxlength="254" value="${fn:escapeXml(pf.email)}" required aria-describedby="profile-email-error" ${not empty fieldErrors.email ? 'aria-invalid="true"' : ''}>
                    </label>
                    <c:if test="${not empty fieldErrors.email}"><div id="profile-email-error" class="invalid-feedback d-block"><c:out value="${fieldErrors.email}" /></div></c:if>
                    <label>Phone (optional)
                        <input class="form-control ${not empty fieldErrors.phone ? 'is-invalid' : ''}" name="phone" type="tel" maxlength="20" value="${fn:escapeXml(pf.phone)}" aria-describedby="profile-phone-error" ${not empty fieldErrors.phone ? 'aria-invalid="true"' : ''}>
                    </label>
                    <c:if test="${not empty fieldErrors.phone}"><div id="profile-phone-error" class="invalid-feedback d-block"><c:out value="${fieldErrors.phone}" /></div></c:if>
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