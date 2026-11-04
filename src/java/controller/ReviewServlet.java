package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.ReviewService;
import java.io.IOException;
import java.util.Set;

@WebServlet("/reviews")
public class ReviewServlet extends BaseServlet {
    private final ReviewService service=new ReviewService();
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException{
        if(r.getSession().getAttribute("user")==null){redirect(r,s,"/login");return;}
        try{r.setAttribute("items",service.customerItems(uid(r)));r.getRequestDispatcher("/WEB-INF/views/customer/reviews.jsp").forward(r,s);}
        catch(Exception e){throw new ServletException(e);}
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException{
        try{
            String action=r.getParameter("action");long user=uid(r);
            if("save".equals(action))service.save(user,Long.parseLong(r.getParameter("itemId")),Integer.parseInt(r.getParameter("productRating")),Integer.parseInt(r.getParameter("sellerRating")),r.getParameter("comment"));
            else if("delete".equals(action))service.delete(user,Long.parseLong(r.getParameter("reviewId")));
            else if("report".equals(action))service.report(user,Long.parseLong(r.getParameter("reviewId")),r.getParameter("reason"),r.getParameter("description"));
            else throw new IllegalArgumentException("Thao tác không hợp lệ.");
            redirect(r,s,"/reviews?saved=1");
        }catch(Exception e){r.setAttribute("error",error(e));doGet(r,s);}
    }
}
