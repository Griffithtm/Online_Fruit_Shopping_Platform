# Demo HoangTM — Sửa giá bán biến thể

## Phạm vi
Demo nhỏ theo MVC: xem các biến thể của Freshora Garden, nhập giá mới, lưu SQL Server.
Dùng cố định seller@freshora.demo để tách khỏi phần đăng nhập của bạn khác. Chỉ chạy demo cục bộ; khi tích hợp phải lấy seller từ session đã đăng nhập và bảo vệ route bằng filter. Đây chưa phải phân quyền đăng nhập hoàn chỉnh.

## Chạy trên NetBeans
1. Database FreshoraDemo đã tồn tại trên SQL Server localhost và có dữ liệu mẫu.
2. Thêm Microsoft SQL Server JDBC driver (.jar tương thích Java 17) vào web/WEB-INF/lib. Nếu thư mục lib chưa có thì tạo nó. Trong NetBeans có thể thêm cùng JAR tại Libraries > Add JAR/Folder. Không thêm servlet-api.jar vào WEB-INF/lib vì Tomcat cung cấp thư viện đó.
3. Mở Windows “Edit environment variables for your account”, tạo ba biến:
   - FRESHORA_DB_URL = jdbc:sqlserver://localhost:1433;databaseName=FreshoraDemo;encrypt=true;trustServerCertificate=true
   - FRESHORA_DB_USER = tài khoản SQL Server của bạn
   - FRESHORA_DB_PASSWORD = mật khẩu SQL Server của bạn
   URL trên giả định SQL Server đã bật TCP/IP và nghe cổng 1433. Máy hiện có instance mặc định MSSQLSERVER, không phải SQLEXPRESS như ảnh mẫu. Chỉnh cổng theo cấu hình SQL Server của máy. trustServerCertificate chỉ dành cho demo cục bộ.
   Tài khoản SQL Server khác tài khoản seller@freshora.demo. Không dùng mật khẩu web Freshora@123 làm mật khẩu SQL Server. Không đưa mật khẩu vào Git.
4. Đóng rồi mở lại NetBeans, khởi động lại Tomcat để nhận biến môi trường. Clean and Build, Run project.
5. Mở http://localhost:9999/Freshora/demo/prices (đổi cổng nếu Tomcat dùng cổng khác).

## Các file cần hiểu
- config/DBConnect.java: đọc cấu hình, tải JDBC driver, mở Connection.
- model/PriceVariant.java: chứa dữ liệu một dòng trên màn hình.
- dao/PriceDAO.java: SELECT danh sách và UPDATE giá bằng PreparedStatement. JOIN Shop và User để giới hạn đúng chủ shop. try-with-resources tự đóng kết nối.
- controller/DemoPriceServlet.java: GET lấy dữ liệu; POST kiểm tra token và giá > 0, gọi DAO, redirect về GET để tránh gửi lại POST khi refresh. BigDecimal dùng cho tiền.
- WEB-INF/views/seller/demo-prices.jsp: hiển thị bảng và form. JSP nằm trong WEB-INF, truy cập thông qua Servlet. Hàm html escape nội dung trước khi hiển thị. JSP dùng vòng lặp Java ngắn để demo không cần thêm JSTL.

## Cách nói với thầy
“Phần em là sửa giá bán biến thể. Khi mở trang, Servlet gọi DAO lấy các biến thể thuộc shop rồi chuyển dữ liệu sang JSP. Khi bấm Save, Servlet nhận ID và giá mới, kiểm tra giá hợp lệ, sau đó DAO dùng PreparedStatement cập nhật SQL Server. Điều kiện UPDATE kiểm tra cả chủ shop để không sửa sản phẩm người khác. Kết quả được tải lại từ database. Demo đang dùng shop cố định; khi ghép nhóm em sẽ lấy người bán từ session đăng nhập.”

## Kịch bản review
1. Trang hiện 3 biến thể của Freshora Garden; không hiện Cam của Green Fruit Shop.
2. Đổi giá Cam từ 35000 sang 39000, Save, refresh và xem giá lưu trong database.
3. Nhập 0 hoặc số âm: bị chặn; server cũng kiểm tra nếu bỏ qua kiểm tra của trình duyệt.
4. Giá có quá 2 chữ số thập phân hoặc quá 16 chữ số phần nguyên: bị từ chối.
5. Giả sửa hidden variantId thành 4 (shop khác): SQL không cập nhật dòng nào.
6. Có thể đổi lại 35000 sau khi demo.

## Đã kiểm tra
- Biên dịch 4 lớp Java thành công với servlet-api của Tomcat 10.1.
- SQL Server có đủ dữ liệu mẫu.
- UPDATE biến thể thuộc shop: 1 dòng; UPDATE biến thể shop khác: 0 dòng. Kiểm tra trong transaction đã rollback, không thay đổi giá mẫu.
- Chưa chạy toàn bộ trang qua JDBC/Tomcat vì chưa tìm thấy JDBC driver trong các thư mục đã kiểm tra và chưa cấu hình tài khoản SQL cho ứng dụng.
## Cập nhật cấu hình kết nối
DBConnect hiện đọc src/java/db.properties thay cho biến môi trường. File này đã có cấu hình cục bộ và được .gitignore bỏ qua. Sau khi chỉnh file, Clean and Build rồi Run. Các thành viên khác sao chép db.properties.example thành db.properties và nhập tài khoản SQL của mình.

