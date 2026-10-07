package controller;
import service.*;import jakarta.servlet.*;import jakarta.servlet.http.*;import jakarta.servlet.annotation.WebServlet;import java.io.*;import java.util.*;
@WebServlet(urlPatterns={"/payment/start","/payment/ipn","/payment/return"})
public class PaymentServlet extends BaseServlet {
 protected void doGet(HttpServletRequest r,HttpServletResponse s)throws IOException,ServletException{
  if(r.getServletPath().equals("/payment/ipn")){
   String code="99";try{Map<String,String> params=new TreeMap<>();for(var e:r.getParameterMap().entrySet()){if(!e.getKey().startsWith("vnp_") || e.getValue().length!=1)throw new IllegalArgumentException("Invalid parameters");params.put(e.getKey(),e.getValue()[0]);}code=new PaymentService().callback(params);}catch(Exception e){getServletContext().log("VNPay IPN failed",e);}
   s.setContentType("application/json;charset=UTF-8");s.getWriter().write("{\"RspCode\":\""+code+"\",\"Message\":\""+(code.equals("00")?"Confirm Success":"Result "+code)+"\"}");return;
  }
  if(r.getServletPath().equals("/payment/return")){r.setAttribute("paymentMessage","Payment is being verified. The signed server notification determines the final status. Check My orders for updates.");view(r,s,"payment");return;}
  s.sendError(405);
 }
 protected void doPost(HttpServletRequest r,HttpServletResponse s)throws IOException,ServletException{
  if(!r.getServletPath().equals("/payment/start")){s.sendError(405);return;}
  try{s.sendRedirect(new PaymentService().start(uid(r),Validation.id(r.getParameter("id")),r.getRemoteAddr()));}catch(Exception e){r.setAttribute("error",error(e));view(r,s,"payment");}
 }
}
