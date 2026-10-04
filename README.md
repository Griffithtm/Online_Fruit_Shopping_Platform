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
- See `ITERATION1.md` for implemented features and current limitations.