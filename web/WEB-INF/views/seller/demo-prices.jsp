<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List,model.PriceVariant"%>
<%!
    // Escape database text before displaying it in HTML.
    private String html(Object value) {
        return value == null ? "" : value.toString().replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<!DOCTYPE html>
<html lang="vi">
    <head><meta charset="UTF-8"><title>Freshora - Cập nhật giá</title>
        <style>body{
            font-family:Arial;
            margin:40px;
            max-width:1050px
        }
        table{
            border-collapse:collapse;
            width:100%
        }
        th,td{
            border:1px solid #ccc;
            padding:12px;
            text-align:left
        }
        th{
            background:#e8f3e9
        }
        input,button{
            padding:8px
        }
        button{
            cursor:pointer
        }
        .notice{
            color:#24562b
        }</style></head>
    <body>
        <h1>Freshora - Cập nhật giá biến thể</h1>
        <p>HoangTM — Người bán minh họa: seller@freshora.demo (Nhà vườn Freshora)</p>
        <p>Bản minh họa sử dụng người bán cố định và không yêu cầu đăng nhập.</p>
        <p class="notice"><%= html(request.getAttribute("message")) %></p>
        <table>
            <tr><th>Mã</th><th>Sản phẩm</th><th>Biến thể</th><th>Giá hiện tại (đ)</th><th>Giá mới (đ)</th></tr>
                    <% List<PriceVariant> variants = (List<PriceVariant>) request.getAttribute("variants");
   if (variants != null) { for (PriceVariant v : variants) { %>
            <tr><td><%=v.getId()%></td><td><%=html(v.getProductName())%></td>
                <td><%=html(v.getVariantName())%></td><td><%=v.getPrice().toPlainString()%></td>
                <td><form method="post" action="<%=request.getContextPath()%>/demo/prices">
                        <input type="hidden" name="token" value="<%=html(session.getAttribute("priceToken"))%>">
                        <input type="hidden" name="variantId" value="<%=v.getId()%>">
                        <input aria-label="Giá mới" type="number" name="price" min="0.01" max="9999999999999999.99" step="0.01" value="<%=v.getPrice().toPlainString()%>" required>
                        <button type="submit">Lưu giá</button></form></td></tr>
                        <% } } %>
        </table>
    </body></html>