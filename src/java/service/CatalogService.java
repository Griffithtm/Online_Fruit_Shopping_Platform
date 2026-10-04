package service;

import config.DBConnect;
import dao.Database;
import java.sql.*;
import java.util.*;

public class CatalogService {

    public List<Map<String, Object>> categories() throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(c, "SELECT id,name FROM settings WHERE type='CATEGORY' AND status='ACTIVE' ORDER BY priority,name");
        }
    }

    public List<Map<String, Object>> products(String q, String category, String store, String min, String max, String rating, String sort, boolean promo) throws Exception {
        String sql = "SELECT p.*,JSON_VALUE(l.image_urls,'$[0]') AS image,l.rating, l.created_at,s.rating AS seller_rating,COALESCE(pop.sold,0) AS sold FROM v_public_products p JOIN listings l ON l.id=p.listing_id JOIN stores s ON s.id=p.store_id OUTER APPLY(SELECT SUM(oi.quantity) sold FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id WHERE oi.variant_id=p.variant_id AND so.status='DELIVERED') pop WHERE 1=1";
        List<Object> a = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            sql += " AND p.title LIKE ?";
            a.add("%" + q.trim() + "%");
        }
        if (category != null && !category.isBlank()) {
            sql += " AND p.category_id=?";
            a.add(Validation.id(category));
        }
        if (store != null && !store.isBlank()) {
            sql += " AND p.store_id=?";
            a.add(Validation.id(store));
        }
        if (min != null && !min.isBlank()) {
            sql += " AND p.price>=?";
            a.add(Validation.money(min, true));
        }
        if (max != null && !max.isBlank()) {
            sql += " AND p.price<=?";
            a.add(Validation.money(max, true));
        }
        if (rating != null && !rating.isBlank()) {
            sql += " AND l.rating>=?";
            a.add(Validation.money(rating, true));
        }
        if (promo) {
            sql += " AND EXISTS(SELECT 1 FROM promotions pr WHERE pr.id=l.campaign_id AND pr.status='RUNNING' AND pr.start_at<=SYSUTCDATETIME() AND pr.end_at>SYSUTCDATETIME())";
        }
        sql += " ORDER BY " + switch (sort == null ? "" : sort) {
            case "priceAsc" ->
                "p.price ASC";
            case "priceDesc" ->
                "p.price DESC";
            case "rating" ->
                "l.rating DESC";
            case "popular" ->
                "sold DESC";
            default ->
                "l.created_at DESC";
        } + ",p.variant_id DESC";
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(c, sql, a.toArray());
        }
    }

    public Map<String, Object> product(long id) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.one(c, "SELECT l.*,s.store_name,s.rating AS seller_rating,s.description AS store_description,JSON_VALUE(l.image_urls,'$[0]') AS image FROM listings l JOIN stores s ON s.id=l.store_id JOIN users u ON u.id=s.owner_id WHERE l.id=? AND l.status='APPROVED' AND s.status='ACTIVE' AND u.status='ACTIVE' AND EXISTS(SELECT 1 FROM variants v JOIN v_available_stock st ON st.variant_id=v.id WHERE v.listing_id=l.id AND v.status='ACTIVE' AND st.available_qty>0)", id);
        }
    }

    public List<Map<String, Object>> variants(long id) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(c, "SELECT v.*,COALESCE(st.available_qty,0) AS available_qty FROM variants v LEFT JOIN v_available_stock st ON st.variant_id=v.id WHERE v.listing_id=? AND v.status='ACTIVE'", id);
        }
    }

    public List<Map<String, Object>> shops() throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(c, "SELECT s.id,s.store_name,s.description,s.rating FROM stores s JOIN users u ON u.id=s.owner_id WHERE s.status='ACTIVE' AND u.status='ACTIVE'");
        }
    }

    public Map<String, Object> shop(long id) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.one(c, "SELECT s.id,s.store_name,s.description,s.rating FROM stores s JOIN users u ON u.id=s.owner_id WHERE s.id=? AND s.status='ACTIVE' AND u.status='ACTIVE'", id);
        }
    }
}
