package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.ReviewService;
import java.io.IOException;

@WebServlet("/admin/review-reports")
public class AdminReviewReportServlet extends BaseServlet {
    private final ReviewService service=new ReviewService();
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException{
        if(r.getSession().getAttribute("user")==null){redirect(r,s,"/login");return;}
        try{if(!"ADMIN".equals(((model.SessionUser)r.getSession().getAttribute("user")).getRole())){s.sendError(403);return;}
            r.setAttribute("reports",service.reportQueue());r.getRequestDispatcher("/WEB-INF/views/admin/review-reports.jsp").forward(r,s);
        }catch(Exception e){throw new ServletException(e);}
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException{
        try{service.moderate(uid(r),Long.parseLong(r.getParameter("reviewId")),r.getParameter("decision"),r.getParameter("note"));redirect(r,s,"/admin/review-reports?saved=1");}
        catch(Exception e){r.setAttribute("error",error(e));doGet(r,s);}
    }
}
