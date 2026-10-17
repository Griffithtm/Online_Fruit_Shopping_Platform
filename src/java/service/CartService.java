package service;

import model.*;

import config.DBConnect;
import dao.Database;
import java.sql.*;
import java.util.*;

public class CartService {

    public List<CartLine> lines(long uid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(CartLine.class, c, "SELECT oi.id,oi.variant_id,oi.quantity,oi.unit_price AS saved_price,v.price AS unit_price,CASE WHEN pub.variant_id IS NULL THEN 0 ELSE 1 END AS purchasable,oi.quantity*v.price AS line_total,l.title,v.option_name,s.store_name,s.id AS store_id,COALESCE(st.available_qty,0) AS available_qty FROM orders o JOIN seller_orders so ON so.order_id=o.id JOIN stores s ON s.id=so.store_id JOIN order_items oi ON oi.seller_order_id=so.id JOIN variants v ON v.id=oi.variant_id JOIN listings l ON l.id=v.listing_id LEFT JOIN v_available_stock st ON st.variant_id=v.id LEFT JOIN v_public_products pub ON pub.variant_id=v.id WHERE o.customer_id=? AND o.status='CART' ORDER BY s.id,oi.id", uid);
        }
    }

    public void change(long uid, String action, long target, int quantity) throws Exception {
        if (!Set.of("add", "update", "remove").contains(action)) {
            throw new IllegalArgumentException("Thao tác không hợp lệ.");
        }
        if (!action.equals("remove") && (quantity < 1 || quantity > 10000)) {
            throw new IllegalArgumentException("Số lượng phải từ 1 đến 10.000.");
        }
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                Database.one(User.class, c, "SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?", uid);
                Order cart = Database.one(Order.class, c, "SELECT id FROM orders WHERE customer_id=? AND status='CART'", uid);
                long oid = cart == null ? Database.insert(c, "INSERT orders(customer_id) VALUES(?)", uid) : cart.getId().longValue();
                if (action.equals("add")) {
                    ProductOffer v = Database.one(ProductOffer.class, c, "SELECT * FROM v_public_products WHERE variant_id=?", target);
                    if (v == null) {
                        throw new IllegalArgumentException("Sản phẩm không còn được bán.");
                    }
                    SellerOrder so = Database.one(SellerOrder.class, c, "SELECT id FROM seller_orders WHERE order_id=? AND store_id=?", oid, v.getStoreId());
                    long sid = so == null ? Database.insert(c, "INSERT seller_orders(order_id,store_id) VALUES(?,?)", oid, v.getStoreId()) : so.getId().longValue();
                    OrderItem line = Database.one(OrderItem.class, c, "SELECT id,quantity FROM order_items WHERE seller_order_id=? AND variant_id=?", sid, target);
                    int total = quantity + (line == null ? 0 : line.getQuantity().intValue());
                    if (total > 10000 || total > v.getAvailableQty().longValue()) {
                        throw new IllegalArgumentException("Số lượng vượt tồn kho hiện tại.");
                    }
                    if (line == null) {
                        Database.update(c, "INSERT order_items(seller_order_id,variant_id,quantity,unit_price) VALUES(?,?,?,?)", sid, target, total, v.getPrice());
                    } else {
                        Database.update(c, "UPDATE order_items SET quantity=?,unit_price=? WHERE id=?", total, v.getPrice(), line.getId());
                    }
                } else {
                    OrderItem line = Database.one(OrderItem.class, c, "SELECT oi.* FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id WHERE oi.id=? AND so.order_id=?", target, oid);
                    if (line == null) {
                        throw new IllegalArgumentException("Dòng giỏ hàng không tồn tại.");
                    }
                    if (action.equals("remove")) {
                        Database.update(c, "DELETE FROM order_items WHERE id=?", target);
                    } else if (action.equals("update")) {
                        ProductOffer v = Database.one(ProductOffer.class, c, "SELECT * FROM v_public_products WHERE variant_id=?", line.getVariantId());
                        if (v == null || quantity > v.getAvailableQty().longValue()) {
                            throw new IllegalArgumentException("Không đủ hàng hoặc sản phẩm đã ngừng bán.");
                        }
                        Database.update(c, "UPDATE order_items SET quantity=?,unit_price=? WHERE id=?", quantity, v.getPrice(), target);
                    } else {
                        throw new IllegalArgumentException("Thao tác không hợp lệ.");
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
