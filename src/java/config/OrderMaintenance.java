package config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import java.util.concurrent.*;
import service.PaymentService;

@WebListener
public class OrderMaintenance implements ServletContextListener {

    private ScheduledExecutorService scheduler;

    public void contextInitialized(ServletContextEvent event) {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "freshora-order-expiry");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                new PaymentService().expireDue();
            } catch (Exception e) {
                event.getServletContext().log("Order expiry failed; retry next cycle", e);
            }
        }, 30, 60, TimeUnit.SECONDS);
    }

    public void contextDestroyed(ServletContextEvent event) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
