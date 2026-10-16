package dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.*;

/** Read-only SQL for the settings table. The caller owns the connection. */
public class SettingDAO {

    /** Names of the active service areas (provinces/cities Freshora delivers to). */
    public List<String> activeServiceAreas(Connection c) throws SQLException {
        List<String> names = new ArrayList<>();
        for (Setting row : Database.rows(Setting.class, c, "SELECT name FROM settings WHERE type='SERVICE_AREA' AND status='ACTIVE' ORDER BY priority,name")) {
            names.add((String) row.getName());
        }
        return names;
    }
}
