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
<html lang="en">
    <head><meta charset="UTF-8"><title>Freshora - Update price</title>
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
        <h1>Freshora - Update Variant Price</h1>
        <p>HoangTM | Local demo seller: seller@freshora.demo (Freshora Garden)</p>
        <p>This demo uses a fixed seller and does not include login.</p>
        <p class="notice"><%= html(request.getAttribute("message")) %></p>
        <table>
            <tr><th>ID</th><th>Product</th><th>Variant</th><th>Current price (VND)</th><th>New price (VND)</th></tr>
                    <% List<PriceVariant> variants = (List<PriceVariant>) request.getAttribute("variants");
   if (variants != null) { for (PriceVariant v : variants) { %>
            <tr><td><%=v.getId()%></td><td><%=html(v.getProductName())%></td>
                <td><%=html(v.getVariantName())%></td><td><%=v.getPrice().toPlainString()%></td>
                <td><form method="post" action="<%=request.getContextPath()%>/demo/prices">
                        <input type="hidden" name="token" value="<%=html(session.getAttribute("priceToken"))%>">
                        <input type="hidden" name="variantId" value="<%=v.getId()%>">
                        <input aria-label="New price" type="number" name="price" min="0.01" max="9999999999999999.99" step="0.01" value="<%=v.getPrice().toPlainString()%>" required>
                        <button type="submit">Save price</button></form></td></tr>
                        <% } } %>
        </table>
    </body></html>