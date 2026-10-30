package listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import service.InventoryAlertService;

/**
 * Runs inventory alerts once at startup and then every 24 hours.
 */
@WebListener
public class InventoryAlertListener implements ServletContextListener {
    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "freshora-inventory-alerts");
            thread.setDaemon(true);
            return thread;
        });
        InventoryAlertService service = new InventoryAlertService();
        scheduler.scheduleWithFixedDelay(service::runChecksSafely, 0, 24, TimeUnit.HOURS);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
