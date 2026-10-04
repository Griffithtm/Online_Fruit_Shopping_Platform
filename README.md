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

- Run `database/FreshoraDB.sql` in SQL Server Management Studio with F5. The installer skips an existing FreshoraDB and does not reset its data.
- Edit `URL`, `USER`, and `PASSWORD` directly in `src/java/config/DBConnect.java` for your machine. The committed password is `CHANGE_ME`.
- Original Sarab source and documentation: `templates/sarab-1.0.0/`.
- Application CSS, scripts, images, and fonts: `web/assets/`.
- Customer and Seller registration are separate; Seller accounts do not require approval. Product listings still require Admin approval.

## Iteration 1 status

Implemented: separate Customer/Seller registration, authentication and role checks, profiles and addresses, listing drafts and Admin review, variants and prices, stock batches and costs, catalog search and comparison, and carts grouped by seller.

This is an initial implementation, not full Iteration 1 acceptance. Remaining work includes Seller autocomplete, normalized unit-price comparison, an explicit stock-availability filter, and more convenient edit forms.

## Runtime notes and limitations

- JDBC and other dependencies are included in `web/WEB-INF/lib`.
- Password recovery writes `.eml` files to `freshora-private-outbox` under Tomcat base. Configure an email provider for actual delivery. `FRESHORA_PUBLIC_URL` controls the recovery-link base URL.
- Uploaded listing images are stored in `freshora-uploads` under Tomcat base. Uploading a replacement image set returns the listing to Draft.
- Sarab banner/About images are template placeholders; product images use uploaded data or a shared fruit illustration.
- Cart operations do not reserve stock. A transactional FEFO reservation service exists; checkout, payments, delivery, and reservation timeout release remain for Iteration 2.
- Sessions expire after 30 minutes of inactivity. Password changes do not yet revoke every session on other devices.
- Legacy `/demo/*` routes are blocked. SQL helpers are in `dao/Database.java`; connection settings are in `config/DBConnect.java`. Some SQL remains in services and AdminServlet.
- Build, JSP compilation, role access, registration, address creation, cart changes, listing approval, and reservation rollback/idempotency have been checked. Concurrent-load and comprehensive security testing are still pending.