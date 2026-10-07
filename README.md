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

- Run `database/FreshoraDB.sql` in SQL Server Management Studio with F5. The script creates FreshoraDB if missing, then applies Iteration 2 schema and English catalog updates without resetting existing business records.
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
- Banner, About and demo product images use local fruit photos. Run database/FreshoraDB.sql to apply the included English demo catalog and image paths.
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
