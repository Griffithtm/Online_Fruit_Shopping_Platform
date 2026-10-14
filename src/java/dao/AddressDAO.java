package dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** SQL for the addresses table (FE-01.2). The caller owns the connection/transaction. */
public class AddressDAO {

    public List<Map<String, Object>> findByUser(Connection c, long uid) throws SQLException {
        return Database.rows(c, "SELECT * FROM addresses WHERE user_id=? ORDER BY is_default DESC,id", uid);
    }

    public boolean existsForUser(Connection c, long id, long uid) throws SQLException {
        return Database.one(c, "SELECT id FROM addresses WHERE id=? AND user_id=?", id, uid) != null;
    }

    /**
     * True when an open order of the user was placed with this address. Orders placed before
     * delivery_address_id existed (NULL) still lock every address of the user.
     */
    public boolean usedByOpenOrder(Connection c, long id, long uid) throws SQLException {
        return Database.one(c, "SELECT TOP 1 id FROM orders WHERE customer_id=? AND status NOT IN ('CART','COMPLETED','CANCELLED','EXPIRED') AND (delivery_address_id=? OR delivery_address_id IS NULL)", uid, id) != null;
    }

    /** True when the address is the pickup address of the user's own store. */
    public boolean isStorePickup(Connection c, long id, long uid) throws SQLException {
        return Database.one(c, "SELECT id FROM stores WHERE owner_id=? AND pickup_address_id=?", uid, id) != null;
    }

    /** True when the user's store still has a seller order to be picked up from this address (READY…ARRIVING, or FAILED awaiting reassignment). */
    public boolean pickupInActiveDelivery(Connection c, long id, long uid) throws SQLException {
        return Database.one(c, "SELECT TOP 1 so.id FROM seller_orders so WITH(HOLDLOCK) JOIN stores s ON s.id=so.store_id WHERE s.owner_id=? AND s.pickup_address_id=? AND so.status IN ('READY','ASSIGNED','PICKED_UP','IN_TRANSIT','ARRIVING','FAILED')", uid, id) != null;
    }

    public int insert(Connection c, long uid, String name, String phone, String line, String ward, String district, String city, boolean primary) throws SQLException {
        return Database.update(c, "INSERT addresses(user_id,recipient_name,phone,address_line,ward,district,city,is_default) VALUES(?,?,?,?,?,?,?,?)",
                uid, name, phone, line, ward, district, city, primary);
    }

    public int update(Connection c, long id, long uid, String name, String phone, String line, String ward, String district, String city, boolean primary) throws SQLException {
        return Database.update(c, "UPDATE addresses SET recipient_name=?,phone=?,address_line=?,ward=?,district=?,city=?,is_default=?,updated_at=SYSUTCDATETIME() WHERE id=? AND user_id=?",
                name, phone, line, ward, district, city, primary, id, uid);
    }

    public int delete(Connection c, long id, long uid) throws SQLException {
        return Database.update(c, "DELETE FROM addresses WHERE id=? AND user_id=?", id, uid);
    }

    public int clearDefault(Connection c, long uid) throws SQLException {
        return Database.update(c, "UPDATE addresses SET is_default=0 WHERE user_id=?", uid);
    }

    public int setDefault(Connection c, long id, long uid) throws SQLException {
        return Database.update(c, "UPDATE addresses SET is_default=1 WHERE id=? AND user_id=?", id, uid);
    }

    public boolean hasDefault(Connection c, long uid) throws SQLException {
        return Database.one(c, "SELECT id FROM addresses WHERE user_id=? AND is_default=1", uid) != null;
    }

    /** Makes the oldest address the default one. */
    public int makeFirstDefault(Connection c, long uid) throws SQLException {
        return Database.update(c, "UPDATE addresses SET is_default=1 WHERE id=(SELECT MIN(id) FROM addresses WHERE user_id=?)", uid);
    }
}
