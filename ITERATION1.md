# Freshora — Iteration 1

Triển khai theo 10 nhóm chức năng Iteration 1 trong Project Tracking. Customer và Seller đăng ký riêng; Seller được tạo cửa hàng ACTIVE ngay. Admin vẫn duyệt **tin đăng sản phẩm**.

## Chạy trong NetBeans

1. Mở project Freshora, dùng JDK 17 và Apache Tomcat 10.1.
2. Database sá»­ dá»¥ng **FreshoraDB**. Sá»­a URL, USER, PASSWORD trá»±c tiáº¿p trong `src/java/config/DBConnect.java`.
3. Clean and Build rồi Run. Trang chính: `http://localhost:9999/Freshora/home` (đổi port nếu cấu hình Tomcat khác).
4. Các thư viện đặt tại `web/WEB-INF/lib`; không cần lấy từ project BookHub.

## Đã triển khai

| Nhóm | Phần hiện có |
|---|---|
| FE01.1 | Đăng ký Customer/Seller riêng, đăng nhập, đăng xuất, session, token khôi phục mật khẩu một lần |
| FE01.2 | Hồ sơ, đổi mật khẩu, thêm/sửa/xóa địa chỉ, địa chỉ mặc định |
| FE01.3 | Kiểm tra vai trò phía server, Admin đổi quyền; Seller có shop ngay |
| FE02.1 | Tạo/sửa draft, gửi duyệt, sửa tin bị từ chối, tải tối đa 4 ảnh và đổi thứ tự |
| FE02.2 | Tạo/sửa/ngừng bán biến thể; tìm sản phẩm tham khảo và sắp giá giảm dần |
| FE02.3 | Nhập lô, ngày nhập/hết hạn, giá vốn, nhật ký tồn; service giữ hàng FEFO trong transaction |
| FE02.4 | Seller sửa giá; Admin chấp thuận/từ chối tin với lý do |
| FE03.1 | Trang chủ, sản phẩm, chi tiết và biến thể, danh sách shop, trang shop |
| FE03.2 | Tìm tên, lọc danh mục/shop/giá/rating/promotion, sắp xếp, bảng so sánh |
| FE04.1 | Thêm/sửa/xóa giỏ, nhóm theo seller, tính tổng theo giá hiện tại |

## Phạm vi và phần cần hoàn thiện

- Đây là bản đầu Iteration 1 để nhóm chạy và tiếp tục hoàn thiện, chưa phải bản nghiệm thu toàn bộ tracking.
- Recovery hiện lưu email `.eml` ở thư mục `freshora-private-outbox` dưới Tomcat base. Cần nối SMTP/provider để gửi email thật. `FRESHORA_PUBLIC_URL` là URL dùng trong liên kết recovery.
- Ảnh upload lưu ngoài WAR tại `freshora-uploads` dưới Tomcat base; ảnh mẫu dùng minh họa trái cây chung. Upload thay thế bộ ảnh và đưa tin về Draft để duyệt lại.
- Gợi ý Seller hiện tìm bằng form, chưa autocomplete khi gõ; bảng so sánh hiển thị giá/quy cách, chưa quy đổi về cùng khối lượng.
- Catalog công khai hiện chỉ lấy sản phẩm có tồn khả dụng. Bộ lọc còn/hết hàng riêng chưa có.
- Service FEFO đã có và đã kiểm tra rollback/idempotency. Checkout, thu tiền, giao hàng và giải phóng giữ hàng theo timeout sẽ được nối trong Iteration 2; giỏ hàng chưa giữ tồn.
- Chưa có kiểm thử tải đồng thời nhiều kết nối hoặc kiểm thử bảo mật toàn diện. Hết phiên sau 30 phút không hoạt động; đổi mật khẩu chưa thu hồi tất cả phiên ở thiết bị khác.
- Tách package controller/service/dao/model/filter; hiện DAO là tiện ích JDBC chung, câu SQL còn ở service và AdminServlet. Có thể tách tiếp DAO theo nghiệp vụ khi nhóm mở rộng.
- Màn hình Seller hỗ trợ nhập ID để sửa; sẽ cần cải thiện thao tác chọn bản ghi và điền form tự động.
- Đường `/demo/*` cũ bị chặn để tránh chức năng demo sửa giá cố định bỏ qua quyền.

## Kiểm thử đã thực hiện

- Biên dịch Java 17.
- Các trang Guest/Customer/Seller/Admin trả HTTP 200; Customer truy cập Seller trả 403.
- Đăng ký hai loại tài khoản độc lập và đăng nhập; Seller tạo draft ngay.
- Thêm địa chỉ; giỏ hàng thêm/sửa/xóa; POST thiếu CSRF trả 403.
- FEFO: gọi lại không giữ trùng, thiếu tồn rollback lần giữ hàng; test dùng transaction rollback.

Code mới ở các package có sẵn trong `src/java`: `controller`, `dao`, `filter`, `model`, `service`, JSP ở `web/WEB-INF/views` (admin, auth, customer, seller; các JSP dùng chung ở views), giao diện dựa trên CSS Sarab tại `web/assets`.
