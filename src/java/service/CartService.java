package service;

import config.DBConnect;
import dao.Database;
import java.sql.*;
import java.util.*;

public class CartService {

    public List<Map<String, Object>> lines(long uid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(c, "SELECT oi.id,oi.variant_id,oi.quantity,oi.unit_price AS saved_price,v.price AS unit_price,CASE WHEN pub.variant_id IS NULL THEN 0 ELSE 1 END AS purchasable,oi.quantity*v.price AS line_total,l.title,v.option_name,s.store_name,s.id AS store_id,COALESCE(st.available_qty,0) AS available_qty FROM orders o JOIN seller_orders so ON so.order_id=o.id JOIN stores s ON s.id=so.store_id JOIN order_items oi ON oi.seller_order_id=so.id JOIN variants v ON v.id=oi.variant_id JOIN listings l ON l.id=v.listing_id LEFT JOIN v_available_stock st ON st.variant_id=v.id LEFT JOIN v_public_products pub ON pub.variant_id=v.id WHERE o.customer_id=? AND o.status='CART' ORDER BY s.id,oi.id", uid);
        }
    }

    public void change(long uid, String action, long target, int quantity) throws Exception {
        if (!Set.of("add","update","remove").contains(action)) throw new IllegalArgumentException("Invalid action.");
        if (!action.equals("remove") && (quantity<1 || quantity>10000)) throw new IllegalArgumentException("Quantity must be between 1 and 10,000.");
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                Database.one(c, "SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?", uid);
                Map<String, Object> cart = Database.one(c, "SELECT id FROM orders WHERE customer_id=? AND status='CART'", uid);
                long oid = cart == null ? Database.insert(c, "INSERT orders(customer_id) VALUES(?)", uid) : ((Number) cart.get("id")).longValue();
                if (action.equals("add")) {
                    Map<String, Object> v = Database.one(c, "SELECT * FROM v_public_products WHERE variant_id=?", target);
                    if (v == null) {
                        throw new IllegalArgumentException("This product is no longer available.");
                    }
                    Map<String, Object> so = Database.one(c, "SELECT id FROM seller_orders WHERE order_id=? AND store_id=?", oid, v.get("store_id"));
                    long sid = so == null ? Database.insert(c, "INSERT seller_orders(order_id,store_id) VALUES(?,?)", oid, v.get("store_id")) : ((Number) so.get("id")).longValue();
                    Map<String, Object> line = Database.one(c, "SELECT id,quantity FROM order_items WHERE seller_order_id=? AND variant_id=?", sid, target);
                    int total = quantity + (line == null ? 0 : ((Number) line.get("quantity")).intValue());
                    if (total > 10000 || total > ((Number) v.get("available_qty")).longValue()) {
                        throw new IllegalArgumentException("The quantity exceeds available stock.");
                    }
                    if (line == null) {
                        Database.update(c, "INSERT order_items(seller_order_id,variant_id,quantity,unit_price) VALUES(?,?,?,?)", sid, target, total, v.get("price"));
                    } else {
                        Database.update(c, "UPDATE order_items SET quantity=?,unit_price=? WHERE id=?", total, v.get("price"), line.get("id"));
                    }
                } else {
                    Map<String, Object> line = Database.one(c, "SELECT oi.* FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id WHERE oi.id=? AND so.order_id=?", target, oid);
                    if (line == null) {
                        throw new IllegalArgumentException("Cart item not found.");
                    }
                    if (action.equals("remove")) {
                        Database.update(c, "DELETE FROM order_items WHERE id=?", target);
                    } else if (action.equals("update")) {
                        Map<String, Object> v = Database.one(c, "SELECT * FROM v_public_products WHERE variant_id=?", line.get("variant_id"));
                        if (v == null || quantity > ((Number) v.get("available_qty")).longValue()) {
                            throw new IllegalArgumentException("Insufficient stock or the product is no longer available.");
                        }
                        Database.update(c, "UPDATE order_items SET quantity=?,unit_price=? WHERE id=?", quantity, v.get("price"), target);
                    } else {
                        throw new IllegalArgumentException("Invalid action.");
                    }
                }
                Database.update(c, "UPDATE so SET subtotal=COALESCE((SELECT SUM(quantity*unit_price) FROM order_items WHERE seller_order_id=so.id),0) FROM seller_orders so WHERE so.order_id=?", oid);
                Database.update(c, "UPDATE orders SET item_total=(SELECT COALESCE(SUM(subtotal),0) FROM seller_orders WHERE order_id=?),payable_amount=(SELECT COALESCE(SUM(subtotal),0) FROM seller_orders WHERE order_id=?),updated_at=SYSUTCDATETIME() WHERE id=?", oid, oid, oid);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }
}
