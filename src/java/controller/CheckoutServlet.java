package controller;
import service.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.util.*;
@WebServlet(urlPatterns={"/checkout","/orders"})
public class CheckoutServlet extends BaseServlet {
 private int parsePoints(String value){try{int n=value==null || value.isBlank()?0:Integer.parseInt(value);if(n<0)throw new NumberFormatException();return n;}catch(NumberFormatException e){throw new IllegalArgumentException("Invalid reward points.");}}
 private final CheckoutService service=new CheckoutService();
 protected void doGet(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException {
  try {
   if(r.getServletPath().equals("/orders")){r.setAttribute("orders",service.orders(uid(r)));r.setAttribute("payments",service.payments(uid(r)));r.setAttribute("items",service.items(uid(r)));view(r,s,"orders");return;}
   r.setAttribute("addresses",new AccountService().addresses(uid(r)));
   if(r.getParameter("address")!=null){
    long address=Validation.id(r.getParameter("address"));
    try {
     Map<String,Object> q=service.quote(uid(r),address,r.getParameter("voucher"),parsePoints(r.getParameter("points")),r.getParameter("method")==null?"COD":r.getParameter("method"));String key=UUID.randomUUID().toString();
     r.getSession().setAttribute("checkoutKey",key);r.getSession().setAttribute("checkoutQuote",q.get("fingerprint"));r.getSession().setAttribute("checkoutAddress",address);r.getSession().setAttribute("checkoutVoucher",r.getParameter("voucher"));r.getSession().setAttribute("checkoutPoints",parsePoints(r.getParameter("points")));r.getSession().setAttribute("checkoutMethod",q.get("method"));
     r.setAttribute("quote",q);r.setAttribute("checkoutKey",key);r.setAttribute("addressId",address);
    }catch(IllegalArgumentException e){r.setAttribute("error",e.getMessage());}
   }
   view(r,s,"checkout");
  }catch(Exception e){throw new ServletException(e);}
 }
 protected void doPost(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException {
  try {
   if(r.getServletPath().equals("/orders")) {
    if(!"cancel".equals(r.getParameter("action")))throw new IllegalArgumentException("Invalid action.");
    new FulfillmentService().change(uid(r),((model.SessionUser)r.getSession().getAttribute("user")).getRole(),Validation.id(r.getParameter("id")),"cancel",null,null,false);redirect(r,s,"/orders");return;
   }
   String key=r.getParameter("key");
   if(key==null || !key.equals(r.getSession().getAttribute("checkoutKey")))throw new IllegalArgumentException("Checkout review expired. Select your address and review again.");
   long id=service.place(uid(r),(Long)r.getSession().getAttribute("checkoutAddress"),key,(String)r.getSession().getAttribute("checkoutQuote"),(String)r.getSession().getAttribute("checkoutVoucher"),(Integer)r.getSession().getAttribute("checkoutPoints"),(String)r.getSession().getAttribute("checkoutMethod"));
   redirect(r,s,"/orders?placed="+id);
  }catch(Exception e){r.setAttribute("error",error(e));doGet(r,s);}
 }
}
