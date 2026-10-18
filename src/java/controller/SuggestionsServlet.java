package controller;

import model.*;

import service.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

@WebServlet("/seller/suggestions")
public class SuggestionsServlet extends BaseServlet {
    protected void doGet(HttpServletRequest r, HttpServletResponse s) throws IOException {
        s.setContentType("application/json;charset=UTF-8");
        try {
            List<ProductOffer> rows = new SellerService().suggestions(uid(r),r.getParameter("q"));
            StringJoiner json = new StringJoiner(",", "[", "]");
            for (ProductOffer p : rows) {
                json.add("{\"title\":"+quote(p.getTitle())+",\"store\":"+quote(p.getStoreName())
                    +",\"pack\":"+quote(p.getOptionName())+",\"price\":"+p.getPrice()
                    +",\"perKg\":"+p.getUnitPriceKg()+",\"id\":"+p.getListingId()+"}");
            }
            s.getWriter().write(json.toString());
        } catch (IllegalArgumentException e) { s.setStatus(400); s.getWriter().write("[]"); }
        catch (Exception e) { getServletContext().log("Suggestions failed",e);s.setStatus(503);s.getWriter().write("[]"); }
    }
    private static String quote(Object value) {
        StringBuilder out=new StringBuilder("\"");
        for(char c:String.valueOf(value).toCharArray()) {
            if(c=='"'||c=='\\')out.append('\\').append(c);
            else if(c<32)out.append(String.format("\\u%04x",(int)c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
}
