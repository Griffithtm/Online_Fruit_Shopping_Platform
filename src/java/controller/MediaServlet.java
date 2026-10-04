package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.file.*;

@WebServlet("/media/*")
public class MediaServlet extends HttpServlet {

    protected void doGet(HttpServletRequest r, HttpServletResponse s) throws IOException {
        String n = r.getPathInfo();
        if (n == null || !n.matches("/[a-f0-9-]{36}\\.png")) {
            s.sendError(404);
            return;
        }
        Path p = Path.of(System.getProperty("catalina.base"), "freshora-uploads", n.substring(1));
        if (!Files.isRegularFile(p)) {
            s.sendError(404);
            return;
        }
        s.setContentType("image/png");
        Files.copy(p, s.getOutputStream());
    }
}
