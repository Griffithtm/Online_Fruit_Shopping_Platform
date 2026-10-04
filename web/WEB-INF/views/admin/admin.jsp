<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <h1>Freshora administration</h1>
        <h2>Listings awaiting review</h2>
        <c:forEach items="${pending}" var="l">
            <section class="panel">
                <h3>#${l.id} <c:out value="${l.title}"/></h3>
                <p>
                    <c:out value="${l.store_name}" /> ·
                    <c:out value="${l.description}" />
                </p>
                <form method="post" action="">
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                    <input type="hidden" name="action" value="review">
                    <input type="hidden" name="id" value="${l.id}">
                    <select class="form-select" name="status">
                        <option value="APPROVED">Approve listing</option>
                        <option value="REJECTED">Reject</option>
                    </select>
                    <label>Reason for rejection
                        <input class="form-control" name="reason" type="text" value="">
                    </label>
                    <button class="button">Confirm</button>
                </form>
            </section>
        </c:forEach>
        <h2>Account roles</h2>
        <p>Sellers register separately with their stores. Seller approval is not required.</p>
        <table class="table">
            <tr>
                <th>Account</th>
                <th>Role</th>
                <th>Update</th>
            </tr>
            <c:forEach items="${users}" var="u">
                <tr>
                    <td>
                        <c:out value="${u.email}" />
                    </td>
                    <td>${u.role}</td>
                    <td>
                        <form method="post" action="">
                            <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                            <input type="hidden" name="action" value="role">
                            <input type="hidden" name="id" value="${u.id}">
                            <select class="form-select" name="role">
                                <option>CUSTOMER</option>
                                <option>DELIVERY_STAFF</option>
                                <option>ADMIN</option>
                            </select>
                            <button class="button">Change role</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
        </table>
        <%@ include file="/WEB-INF/views/footer.jsp" %>