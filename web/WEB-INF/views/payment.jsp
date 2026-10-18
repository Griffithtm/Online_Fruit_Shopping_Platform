<%@ page pageEncoding="UTF-8" %><%@ include file="/WEB-INF/views/header.jsp" %>
<h1>Trạng thái thanh toán</h1><p><c:out value="${paymentMessage}"/></p><a class="button" href="${pageContext.request.contextPath}/orders">Xem đơn hàng của tôi</a><%@ include file="/WEB-INF/views/footer.jsp" %>
