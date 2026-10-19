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

    /** Active service areas with their delivery fee and optional eta_hours from the JSON value (null when missing or invalid). */
    public List<ServiceArea> activeServiceAreaRows(Connection c) throws SQLException {
        return Database.rows(ServiceArea.class, c, "SELECT name,TRY_CONVERT(decimal(15,2),JSON_VALUE(CASE WHEN ISJSON(value)=1 THEN value ELSE '{}' END,'$.delivery_fee')) AS fee,"
                + "TRY_CONVERT(int,JSON_VALUE(CASE WHEN ISJSON(value)=1 THEN value ELSE '{}' END,'$.eta_hours')) AS eta_hours"
                + " FROM settings WHERE type='SERVICE_AREA' AND status='ACTIVE' ORDER BY priority,name");
    }
}
