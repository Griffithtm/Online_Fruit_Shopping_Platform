package dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.StringJoiner;

/** Append-only audit trail (GB-06, NFR-AUD-01). Only INSERT; the caller owns the transaction. */
public class AuditDAO {

    public long log(Connection c, Long actorId, String entityType, long entityId, String action, String oldValue, String newValue, String note) throws SQLException {
        return Database.insert(c, "INSERT audit_logs(actor_id,entity_type,entity_id,action,old_value,new_value,note) VALUES(?,?,?,?,?,?,?)",
                actorId, entityType, entityId, action, oldValue, newValue, note);
    }

    /** Small JSON object of the given fields, in iteration order; null values are written as null. */
    public static String json(Map<String, ?> fields) {
        StringJoiner out = new StringJoiner(",", "{", "}");
        fields.forEach((k, v) -> out.add(quote(k) + ":" + (v == null ? "null" : v instanceof Boolean || v instanceof Number ? v.toString() : quote(v.toString()))));
        return out.toString();
    }

    private static String quote(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        b.append(String.format("\\u%04x", (int) ch));
                    } else {
                        b.append(ch);
                    }
                }
            }
        }
        return b.append('"').toString();
    }
}
