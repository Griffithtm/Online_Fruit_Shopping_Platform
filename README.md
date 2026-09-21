# Freshora

Fruit marketplace built with Java Servlets and JSP using MVC.

## Development setup

- Apache NetBeans IDE 17
- JDK 17 (project source/target)
- Apache Tomcat compatible with Jakarta Servlet 6.0 (Tomcat 10.1)
- Ant web application project
- SQL Server is planned; database integration is not implemented yet.

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

HomeServlet currently forwards /home to /WEB-INF/views/home.jsp.

Build output and personal NetBeans settings are ignored. Configure credentials locally; do not commit passwords or tokens.
