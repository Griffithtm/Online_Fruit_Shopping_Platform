# Freshora

Fruit marketplace built with Java Servlets and JSP using MVC.

## Development setup

- Apache NetBeans IDE 17
- JDK 17 (project source/target)
- Apache Tomcat compatible with Jakarta Servlet 6.0 (Tomcat 10.1)
- Ant web application project
- SQL Server with FreshoraDB (schema and demo data included).

Open this folder as an existing project in NetBeans. Configure your local JDK and Tomcat under Project Properties, then Clean and Build and Run.

Open /Freshora/home on your configured Tomcat port. Example: http://localhost:9999/Freshora/home

## Structure

- src/java/controller: request handling with Servlets
- src/java/model: domain classes
- src/java/dao: database access
- src/java/service: business logic
- src/java/config: application/database configuration
- src/java/filter: authentication and authorization filters
- src/java/util: shared utilities
- web/WEB-INF/views: JSP views
- web/assets: CSS, JavaScript and images

HomeController forwards /home to /WEB-INF/views/home.jsp.

Build output and personal NetBeans settings are ignored. Configure credentials locally; do not commit passwords or tokens.

## Database and template

- Run `database/FreshoraDB.sql` in SQL Server Management Studio with F5. The script creates FreshoraDB if missing, then applies Iteration 2 schema and Vietnamese catalog updates without resetting existing business records.
- Edit `URL`, `USER`, and `PASSWORD` directly in `src/java/config/DBConnect.java` for your machine. The committed password is `CHANGE_ME`.
- Original Sarab source and documentation: `templates/sarab-1.0.0/`.
- Application CSS, scripts, images, and fonts: `web/assets/`.
- Customer and Seller registration are separate; Seller accounts do not require approval. Product listings still require Admin approval.

## Iteration 1 status

Implemented: separate Customer/Seller registration, authentication and role checks, profiles and addresses, listing drafts and Admin review, variants and prices, stock batches and costs, catalog search and comparison, and carts grouped by seller.

Iteration 1 additions: live suggestions from other stores ordered by price, prefilled listing/variant edit forms, in-stock/out-of-stock/all filters, seller filters, and comparison sorted by VND/kg for explicitly weighted packs. Unknown or ambiguous weights are shown as unspecified. Delivery service areas and fees use the configured platform settings.

Customer and Seller registration remain separate. Iteration 2 adds checkout, fulfillment, delivery, promotions, points, refunds and settlement summaries. The Iteration 1 stock service is tested independently for concurrent allocation, FEFO, rollback and idempotency.

## Runtime notes and limitations

- JDBC and other dependencies are included in `web/WEB-INF/lib`.
- Password recovery supports Gmail SMTP with STARTTLS and an App Password. See configuration below. Explicit local outbox mode is also available.
- Uploaded listing images are stored in `freshora-uploads` under Tomcat base. Uploading a replacement image set returns the listing to Draft.
- Banner, About and demo product images use local fruit photos. Run database/FreshoraDB.sql to apply the included Vietnamese demo catalog and image paths.
- Cart operations do not reserve stock. Checkout reserves stock with transactional FEFO allocation. Cancellation, rejection and payment expiry release holds; delivery consumes them.
- Sessions expire after 30 minutes of inactivity. Password changes and recovery invalidate previous sessions on their next request.
- Legacy `/demo/*` routes are blocked. SQL helpers are in `dao/Database.java`; connection settings are in `config/DBConnect.java`. Some SQL remains in services and AdminServlet.
- Java compilation, Tomcat JSP rendering, HTTP account flows, role/CSRF restrictions, SMTP recovery, token reuse prevention, seller ownership, stock filtering, unit prices, carts, and two simultaneous stock reservations have been checked. This is functional/integration verification, not a production load or penetration test.
## Gmail password recovery

Set these environment variables on the machine running Tomcat, then restart Tomcat (and NetBeans if it starts Tomcat). Never put the App Password in Git:

- `FRESHORA_SMTP_USER`: the Gmail address sending recovery emails.
- `FRESHORA_SMTP_PASSWORD`: that account's App Password, without display spaces.
- `FRESHORA_PUBLIC_URL`: the application's reachable base URL, for example `http://localhost:9999/Freshora` for local development.
- Optional: `FRESHORA_SMTP_HOST=smtp.gmail.com`, `FRESHORA_SMTP_PORT=587`, `FRESHORA_MAIL_MODE=smtp` (defaults).

External SMTP requires STARTTLS and certificate hostname validation. Missing credentials or delivery errors fail explicitly and invalidate the unsent recovery token. Actual Gmail delivery needs your configured account; local SMTP integration tests do not prove Gmail account access.

For offline development only, set `FRESHORA_MAIL_MODE=outbox` to write email files under `freshora-private-outbox` in Tomcat's base folder. Do not expose this directory as web content.

## Iteration 1 verification

`test/IterationOneChecks.java` uses DBConnect to create uniquely named test records, checks services and concurrent stock allocation, and removes its fixtures. Run against a development database only. Compile it with the application's classes and `web/WEB-INF/lib/*` on the classpath, then run `IterationOneChecks`.

`test/http_checks.py` targets a separate test Tomcat at port 10109 and a loopback SMTP receiver at port 10259. Start that Tomcat with `FRESHORA_SMTP_HOST=127.0.0.1`, `FRESHORA_SMTP_PORT=10259`, and `FRESHORA_PUBLIC_URL=http://localhost:10109/Freshora`; leave SMTP user/password unset. It uses the supplied seller/admin demo accounts plus a uniquely named customer test account. Run `CleanupHttpChecks tmp/iter1-http-emails.txt` with the Java test classpath afterward to remove the created accounts and tokens.

Verification on 2026-10-07: 18 service assertions and 28 HTTP/SMTP checks passed. Gmail delivery awaits local credentials. No database reset or schema migration is required for these additions.


## Iteration 2 setup

For both a new installation and an existing database, run the single script `database/FreshoraDB.sql` before deploying this version. It is safe to rerun and preserves existing records. The current local database has been upgraded. Rebuild and redeploy the WAR.

Routes: `/checkout`, `/orders`, `/wallet`, `/refunds`, `/seller/orders`, `/seller/promotions`, `/admin/orders`, `/admin/promotions`, `/admin/refunds`, `/delivery`.

### VNPay sandbox

Set these variables for the process starting Tomcat, then restart Tomcat/NetBeans:

- `FRESHORA_VNPAY_TMN_CODE`: sandbox website code.
- `FRESHORA_VNPAY_HASH_SECRET`: sandbox signing secret; never commit it.
- `FRESHORA_PUBLIC_URL`: externally reachable HTTPS application URL including `/Freshora`.

Register `<FRESHORA_PUBLIC_URL>/payment/ipn` with VNPay. Browser return is `/payment/return` and cannot confirm payment. Card uses INTCARD; QR/wallet uses VNPAYQR and the payment methods enabled on the merchant account. COD works without VNPay configuration.

Payment/refund implementation references:
https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr&refund.html

Live sandbox payment/refund requests require merchant credentials and reachable IPN. Local signed-fixture tests do not establish provider acceptance. An approved refund is not yet completed. COD completion needs evidence. Online refunds are submitted once; uncertain outcomes stay Pending. Admin may reconcile a Pending refund with its signed VNPay response. Settlement records a summary, not a bank payout.

### Policy defaults for review

Settings POLICY defaults: cancellation 30 minutes; delivery ETA 24 hours; payment timeout 15 minutes; one earned point per 10,000 VND eligible delivered merchandise; one redeemed point worth 1,000 VND; points expire after 365 days; refund window 7 days. These defaults fill numerical gaps in the SRS and should be reviewed before acceptance. Commission is snapshotted from the store tier at placement.

Delivery fee is split equally between sellers; the last seller receives the rounding remainder. Voucher discount is proportional after seller campaigns. Points use whole units; checkout requests fewer points if they cannot fit the seller allocation. Cancellation returns allocated spent points as a new expiring lot. Completed refunds reverse unspent earned points; already-spent earned points are retained under the current policy. Promotion budget/use is restored on whole-order cancellation or expiry. Original order totals remain snapshots; the order page separately shows current payable after seller cancellations.

### Regression tests

- `test/IterationTwoChecks.java`: COD fulfillment, delivery, refunds, points and voucher limits.
- `test/PaymentChecks.java`: test-signed VNPay payment/refund messages, tampering, replay, expiry and late payment. Use test-only VNPay env values and an example.invalid HTTPS URL for this process; no provider request is sent.
- `test/BenefitsStockChecks.java`: points, campaign budgets and concurrent checkout for one unit.
- `test/iteration2_http_views.py`: JSP and role guards on the isolated test server at port 10109.

Compile Java tests against `build/web/WEB-INF/classes` and `web/WEB-INF/lib/*` with a separate output directory and empty sourcepath. Tests create I2-prefixed fixtures and preserve immutable financial history. Fixture users are suspended and listings hidden afterward; use a disposable database for repeated runs.

Membership uses qualifying delivered merchandise spending over a configurable rolling `membership_period_days` (default 365), excluding refunded seller orders. The wallet shows next-tier progress; actual tier changes generate notifications. Missing eligible tier rules retain the current tier and show a warning. Existing order prices remain unchanged.

## Iterations 1 and 2 audit (2026-10-11)

Reviewed against CRMS rows 2-90 in Project Tracking. The old seller-application and approval entries are superseded by the requested separate Customer/Seller registration; selling access does not await Admin approval.

| Scope | Implemented flows | Verification |
| --- | --- | --- |
| Iteration 1: FE-01 | Accounts, sessions, recovery, profiles, addresses, role/store restrictions | Service and HTTP checks; local SMTP recovery |
| Iteration 1: FE-02 | Listing drafts, images/reordering, review, variants, competitive suggestions, pricing, batches and atomic reservation | Code review, service and HTTP checks |
| Iteration 1: FE-03 / FE-04.1 | Browsing, filtering, sorting, comparison, seller-grouped carts | Service and HTTP checks |
| Iteration 2: FE-04.2 / FE-05 | Revalidation, checkout totals, holds/releases, order history/cancellation, seller fulfillment | Service, concurrency and HTTP checks |
| Iteration 2: FE-06 | COD, signed VNPay messages, refunds, commission and settlement | Local signed fixtures; real VNPay sandbox acceptance remains pending |
| Iteration 2: FE-07 | Assignment, pickup, delivery events, evidence and COD collection | Service state-transition checks and role guards |
| Iteration 2: FE-08 / FE-09 | Campaigns, vouchers, redemption limits, points ledger and membership | Service, concurrency and HTTP checks |

This audit fixed resubmission after refund rejection, repeated completed-refund requests reopening disputes, legacy cancellation bypassing time limits and benefit restoration, and exhausted automatic campaigns blocking checkout. Automatic campaigns now fall back to normal price when ineligible; explicitly entered invalid vouchers still report validation errors.

Duplicate-payment compensation uses `transactions.refund_scope=EXCESS_PAYMENT`; completing it does not reverse purchase rewards or reduce seller settlement. Purchase refunds use `PURCHASE`. Run the single `database/FreshoraDB.sql` before deploying this update. The script was tested both as a fresh install and a repeat upgrade on a separate audit database. Existing historical refund rows default to PURCHASE; investigate any historical duplicate-payment compensation separately before settlement.

The regression run passed 109 Java assertions and 52 HTTP/SMTP checks on an isolated database, including `test/RefundLifecycleChecks.java`. These are targeted regression checks, not proof of every possible input or provider behavior. The application builds successfully. Real Gmail delivery and real VNPay sandbox payment/refund/IPN still require configured service credentials and a reachable callback URL. Do not mark those external acceptance items Done based only on fixtures.

## Giao diện và dữ liệu tiếng Việt (14/10/2026)

Giao diện các vai trò, thông báo xác thực, thư khôi phục mật khẩu, nhãn trạng thái và dữ liệu mẫu đã được Việt hóa. Tiền được định dạng theo vi_VN. Tên trường, mã trạng thái, đường dẫn, tài khoản đăng nhập và giao thức VNPay được giữ nguyên để bảo toàn tích hợp.

Chạy file duy nhất `database/FreshoraDB.sql` cho cả cài mới và nâng cấp. Phần cập nhật tiếng Việt chỉ đổi các trường văn bản đã biết, không xóa dữ liệu và không thay đổi số tiền, số lượng hay khóa tham chiếu. Biên dịch và triển khai lại ứng dụng sau khi cập nhật. Nội dung tự nhập mới được giữ nguyên theo người nhập.


## Model JavaBean (14/10/2026)

Package `model` có lớp riêng cho 22 bảng và 2 view của database: `User`, `Address`, `Store`, `Listing`, `Variant`, `Batch`, `Order`, `SellerOrder`, `OrderItem`, `Transaction`, `Promotion`, `PromotionRedemption`, `Setting`, `Subscription`, `Review`, `ReviewReport`, `Notification`, `PasswordResetToken`, `StockMovement`, `IntegrationEvent`, `AuditLog`, `SchemaVersion`, `AvailableStock`, `ProductOffer`.

Các lớp `...Details`/`OrderSummary` chứa kết quả truy vấn JOIN; `CartLine`, `CheckoutLine`, `CheckoutQuote`, `CheckoutSellerTotal`, `Wallet`, `MembershipProgress` chứa dữ liệu tổng hợp theo nghiệp vụ. `PromotionForm` nhận các trường từ biểu mẫu khuyến mãi. `SessionUser` tiếp tục chỉ lưu thông tin phiên đăng nhập. Model chứa dữ liệu; truy cập SQL ở DAO và xử lý nghiệp vụ ở service.

`Database.one(User.class, connection, sql, args...)` và `Database.rows(User.class, connection, sql, args...)` ánh xạ tên cột SQL `snake_case` sang thuộc tính JavaBean `camelCase`. Cột không có thuộc tính tương ứng hoặc chuyển số làm mất độ chính xác sẽ báo lỗi. DAO/service/controller không còn dùng `Map<String, Object>` để đại diện đối tượng nghiệp vụ. Map vẫn dùng đúng mục đích cho tham số VNPay và bảng tra cứu theo mã đơn/khuyến mãi. JSP đọc các thuộc tính như `${order.paymentMethod}`.

Chuyển đổi này không đổi schema hoặc dữ liệu database. Biên dịch và triển khai lại toàn bộ ứng dụng; trang checkout mở trước khi triển khai cần tải lại để nhận báo giá mới. `test/ModelMappingChecks.java` kiểm tra schema, kiểu số, NULL, Unicode và dấu vết địa chỉ checkout; chỉ chạy với database kiểm thử có tên chứa `Verify`. `TestRows` và `TestForms` trong thư mục test là tiện ích kiểm thử, không được đóng gói vào ứng dụng.

## Yêu cầu hậu kiểm sản phẩm — 15/10/2026 (chưa triển khai)

Theo góp ý của giảng viên: Admin có trách nhiệm hậu kiểm sản phẩm, không phải duyệt tất cả sản phẩm trước khi Seller đăng bán. Seller đăng sản phẩm đáp ứng kiểm tra dữ liệu tự động thì sản phẩm được công khai; Admin kiểm tra sau khi đăng, chủ động hoặc khi có báo cáo, và xử lý sản phẩm vi phạm kèm lý do. Cần sửa luồng chờ duyệt bắt buộc hiện tại trong code, SRS và SDS khi triển khai yêu cầu này.

Đề xuất để thảo luận: dùng DRAFT (bản nháp), PUBLISHED (đã đăng), HIDDEN (Seller ẩn), SUSPENDED (Admin đình chỉ). Việc Seller sửa sản phẩm bị đình chỉ có cần Admin cho phép khôi phục hay không vẫn cần thống nhất; đây chưa phải quyết định đã được xác nhận. Commit hiện tại chỉ lưu yêu cầu, chưa thay đổi luồng duyệt sản phẩm.
## Tích hợp nhánh Thanh — 16/10/2026

Đã tích hợp các commit 8c5c984, 863ac73, a69dae8 và e6c0274: DAO tài khoản/địa chỉ, kiểm tra hồ sơ và email trùng, khóa địa chỉ theo đơn hàng, quy tắc địa chỉ mặc định, kiểm tra mật khẩu và nhật ký thay đổi. Phần tích hợp sử dụng JavaBean hiện tại và thông báo tiếng Việt; giữ lại quyền đăng ký Seller riêng và ghi chú hậu kiểm chưa triển khai.

Trước khi chạy bản này, chạy toàn bộ `database/FreshoraDB.sql` để thêm `orders.delivery_address_id` và chỉ mục liên quan. Script nâng cấp không xóa dữ liệu. Không bỏ qua bước này: checkout và sổ địa chỉ mới sử dụng cột đó. Sau đó Clean and Build rồi triển khai lại ứng dụng.

Đã kiểm thử trên database Verify riêng: bộ kiểm thử tài khoản mở rộng của Thành; các bộ giỏ hàng/tồn kho, Iteration 2, khuyến mãi/điểm, thanh toán/hoàn tiền, ánh xạ model; 52 kiểm tra HTTP/SMTP hiện có và 8 kiểm tra form tài khoản sau merge. Script `test/account_merge_checks.py` chạy trên server kiểm thử cổng 10109, tạo một tài khoản @test.invalid riêng cho mỗi lần chạy.