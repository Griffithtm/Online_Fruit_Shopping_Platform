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
        <c:if test="${canManageAddresses}">
        <h2>Address book</h2>
        <div class="productgrid">
            <c:forEach items="${addresses}" var="a">
                <c:set var="fid" value="${a.id}" />
                <c:set var="isNewForm" value="false" />
                <c:set var="failed" value="${addressFormId == fid.toString()}" />
                <c:set var="af" value="${failed ? addressForm : a}" />
                <c:set var="fe" value="${failed ? fieldErrors : null}" />
                <section class="panel">
                    <c:choose>
                        <c:when test="${a.is_default}">
                            <p><span class="badge bg-success">Default address</span></p>
                        </c:when>
                        <c:otherwise>
                            <form method="post" action="">
                                <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                                <input type="hidden" name="action" value="defaultAddress">
                                <input type="hidden" name="id" value="${a.id}">
                                <button class="button">Set as default</button>
                            </form>
                        </c:otherwise>
                    </c:choose>
                    <form method="post" action="" novalidate>
                        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                        <input type="hidden" name="action" value="saveAddress">
                        <input type="hidden" name="id" value="${a.id}">
                        <%@ include file="/WEB-INF/views/customer/address-form.jspf" %>
                        <button class="button">Update</button>
                    </form>
                    <form method="post" action="" data-confirm="${fn:escapeXml(deleteAddressConfirm)}">
                        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                        <input type="hidden" name="action" value="deleteAddress">
                        <input type="hidden" name="id" value="${a.id}">
                        <button class="button">Delete address</button>
                    </form>
                </section>
            </c:forEach>
            <section class="panel">
                <h3>Add address</h3>
                <c:set var="fid" value="new" />
                <c:set var="isNewForm" value="true" />
                <c:set var="failed" value="${addressFormId == 'new'}" />
                <c:set var="af" value="${failed ? addressForm : null}" />
                <c:set var="fe" value="${failed ? fieldErrors : null}" />
                <form method="post" action="" novalidate>
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                    <input type="hidden" name="action" value="saveAddress">
                    <%@ include file="/WEB-INF/views/customer/address-form.jspf" %>
                    <button class="button">Add address</button>
                </form>
            </section>
        </div>
        <script>
            // "Other" shows the free-text province box; without JavaScript the box simply stays visible.
            document.querySelectorAll('[data-city-select]').forEach(function (select) {
                var other = select.closest('form').querySelector('[data-city-other]');
                var sync = function () { other.hidden = select.value !== '__other'; };
                select.addEventListener('change', sync);
                sync();
            });
            document.querySelectorAll('form[data-confirm]').forEach(function (form) {
                form.addEventListener('submit', function (e) {
                    if (!window.confirm(form.getAttribute('data-confirm'))) { e.preventDefault(); }
                });
            });
        </script>
        </c:if>
        <%@ include file="/WEB-INF/views/footer.jsp" %>
