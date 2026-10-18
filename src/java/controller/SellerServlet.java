package controller;

import model.*;

import service.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

@WebServlet("/seller")
@MultipartConfig(maxFileSize = 5242880, maxRequestSize = 22020096)
public class SellerServlet extends BaseServlet {

    private final SellerService service = new SellerService();

    protected void doGet(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try {
            r.setAttribute("listings", service.listings(uid(r)));
            r.setAttribute("variants", service.variants(uid(r)));
            r.setAttribute("batches", service.batches(uid(r)));
            r.setAttribute("categories", new CatalogService().categories());
            r.setAttribute("suggestions", service.suggestions(uid(r),r.getParameter("q")));
            if (r.getParameter("images") != null) {
                r.setAttribute("images", service.images(uid(r), Validation.id(r.getParameter("images"))));
            }
            if(r.getParameter("editListing")!=null) r.setAttribute("editListing", service.listings(uid(r)).stream().filter(x -> x.getId().toString().equals(r.getParameter("editListing"))).findFirst().orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin đăng.")));
            if(r.getParameter("editVariant")!=null) r.setAttribute("editVariant", service.variants(uid(r)).stream().filter(x -> x.getId().toString().equals(r.getParameter("editVariant"))).findFirst().orElseThrow(() -> new IllegalArgumentException("Biến thể không tồn tại.")));
            view(r, s, "seller");
        } catch (Exception e) {
            r.setAttribute("error", error(e));
            view(r, s, "seller");
        }
    }

    protected void doPost(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try {
            String a = r.getParameter("action");
            String id = r.getParameter("id");
            switch (a) {
                case "listing" ->
                    service.listing(uid(r), id, r.getParameter("title"), r.getParameter("description"), r.getParameter("category"), r.getParameter("origin"), r.getParameter("harvest"));
                case "submit" ->
                    service.submit(uid(r), Validation.id(id));
                case "variant", "retire" ->
                    service.variant(uid(r), id, Validation.id(r.getParameter("listing")), r.getParameter("option"), r.getParameter("unit"), r.getParameter("price"), a.equals("retire"));
                case "stock" ->
                    service.stock(uid(r), Validation.id(r.getParameter("variant")), r.getParameter("batch"), r.getParameter("origin"), r.getParameter("harvest"), r.getParameter("receipt"), r.getParameter("expiry"), r.getParameter("supplier"), r.getParameter("quantity"), r.getParameter("cost"));
                case "upload" -> {
                    long lid = Validation.id(id);
                    boolean owned = service.listings(uid(r)).stream().anyMatch(l -> l.getId().longValue() == lid);
                    if (!owned) {
                        throw new IllegalArgumentException("Tin đăng không thuộc cửa hàng.");
                    }
                    List<String> paths = new ArrayList<>();
                    Path dir = Path.of(System.getProperty("catalina.base"), "freshora-uploads");
                    Files.createDirectories(dir);
                    for (Part part : r.getParts()) {
                        if (part.getName().equals("photos") && part.getSize() > 0) {
                            if (paths.size() >= 4) {
                                throw new IllegalArgumentException("Tối đa 4 ảnh.");
                            }
                            java.awt.image.BufferedImage img;
                            try (InputStream in = part.getInputStream()) {
                                img = ImageIO.read(in);
                            }
                            if (img == null || img.getWidth() > 6000 || img.getHeight() > 6000) {
                                throw new IllegalArgumentException("Chỉ nhận ảnh hợp lệ, tối đa 6000px.");
                            }
                            String name = UUID.randomUUID() + ".png";
                            ImageIO.write(img, "png", dir.resolve(name).toFile());
                            paths.add("/media/" + name);
                        }
                    }
                    if (paths.isEmpty()) {
                        throw new IllegalArgumentException("Chọn ảnh cần tải.");
                    }
                    service.images(uid(r), lid, paths);
                }
                case "reorder" -> {
                    long lid = Validation.id(id);
                    List<String> paths = new ArrayList<>(service.images(uid(r), lid).stream().map(m -> m.getPath()).toList());
                    int index = Integer.parseInt(r.getParameter("position"));
                    if (index > 0 && index < paths.size()) {
                        Collections.swap(paths, index, index - 1);
                    }
                    service.images(uid(r), lid, paths);
                }
                default ->
                    throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
            redirect(r, s, "/seller?saved=1");
        } catch (Exception e) {
            r.setAttribute("error", error(e));
            doGet(r, s);
        }
    }
}
