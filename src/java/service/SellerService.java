package service;

import config.DBConnect;
import dao.Database;
import java.sql.*;
import java.util.*;
import java.time.LocalDate;
import java.math.BigDecimal;

public class SellerService {

    public List<Map<String,Object>> suggestions(long uid,String query) throws Exception {
        if(query==null || query.trim().length()<2) return List.of();
        if(query.length()>200) throw new IllegalArgumentException("Product name is too long.");
        long own;
        try(Connection c=new DBConnect().getConnection()){own=store(c,uid);}
        return new CatalogService().products(query,null,null,null,null,null,"priceDesc",false).stream()
            .filter(x -> ((Number)x.get("store_id")).longValue()!=own).limit(10).toList();
    }

    public long store(Connection c, long uid) throws Exception {
        Map<String, Object> s = Database.one(c, "SELECT id FROM stores WHERE owner_id=? AND status='ACTIVE'", uid);
        if (s == null) {
            throw new IllegalArgumentException("Your store is not active.");
        }
        return ((Number) s.get("id")).longValue();
    }

    public List<Map<String, Object>> listings(long uid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(c, "SELECT * FROM listings WHERE store_id=? ORDER BY id DESC", store(c, uid));
        }
    }

    public List<Map<String, Object>> variants(long uid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(c, "SELECT v.*,l.title,COALESCE(st.available_qty,0) AS available_qty FROM variants v JOIN listings l ON l.id=v.listing_id LEFT JOIN v_available_stock st ON st.variant_id=v.id WHERE l.store_id=? ORDER BY v.id DESC", store(c, uid));
        }
    }

    public List<Map<String, Object>> batches(long uid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(c, "SELECT b.*,v.option_name,l.title FROM batches b JOIN variants v ON v.id=b.variant_id JOIN listings l ON l.id=v.listing_id WHERE l.store_id=? ORDER BY b.id DESC", store(c, uid));
        }
    }

    public void listing(long uid, String id, String title, String desc, String cat, String origin, String harvest) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            long sid = store(c, uid), category = Validation.id(cat);
            title = Validation.text(title, 200);
            origin = Validation.text(origin, 100);
            if (Database.one(c, "SELECT id FROM settings WHERE id=? AND type='CATEGORY' AND status='ACTIVE'", category) == null) {
                throw new IllegalArgumentException("Invalid category.");
            }
            if (id == null || id.isBlank()) {
                Database.update(c, "INSERT listings(store_id,category_id,title,description,origin,harvest_info) VALUES(?,?,?,?,?,?)", sid, category, title, desc, origin, harvest);
            } else if (Database.update(c, "UPDATE listings SET title=?,description=?,category_id=?,origin=?,harvest_info=?,status='DRAFT',rejection_reason=NULL,updated_at=SYSUTCDATETIME() WHERE id=? AND store_id=? AND status<>'RETIRED'", title, desc, category, origin, harvest, Validation.id(id), sid) != 1) {
                throw new IllegalArgumentException("No matching listing found in your store.");
            }
        }
    }

    public void submit(long uid, long id) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            if (Database.update(c, "UPDATE listings SET status='PENDING_REVIEW',rejection_reason=NULL,updated_at=SYSUTCDATETIME() WHERE id=? AND store_id=? AND status IN ('DRAFT','REJECTED') AND EXISTS(SELECT 1 FROM variants WHERE listing_id=listings.id AND status='ACTIVE')", id, store(c, uid)) != 1) {
                throw new IllegalArgumentException("At least one active variant is required. The listing must be Draft or Rejected.");
            }
        }
    }

    public void variant(long uid, String id, long listing, String option, String unit, String price, boolean retire) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            long sid = store(c, uid);
            if (Database.one(c, "SELECT id FROM listings WHERE id=? AND store_id=?", listing, sid) == null) {
                throw new IllegalArgumentException("The listing does not belong to your store.");
            }
            if (retire) {
                if (Database.update(c, "UPDATE variants SET status='RETIRED',updated_at=SYSUTCDATETIME() WHERE id=? AND listing_id=?", Validation.id(id), listing) != 1) {
                    throw new IllegalArgumentException("Variant not found.");
                }
                return;
            }
            BigDecimal amount = Validation.money(price, false);
            option = Validation.text(option, 100);
            unit = Validation.text(unit, 20);
            if (id == null || id.isBlank()) {
                Database.update(c, "INSERT variants(listing_id,option_name,unit,price) VALUES(?,?,?,?)", listing, option, unit, amount);
            } else if (Database.update(c, "UPDATE variants SET option_name=?,unit=?,price=?,updated_at=SYSUTCDATETIME() WHERE id=? AND listing_id=? AND status='ACTIVE'", option, unit, amount, Validation.id(id), listing) != 1) {
                throw new IllegalArgumentException("Variant not found.");
            }
        }
    }

    public void stock(long uid, long variant, String no, String origin, String harvest, String receipt, String expiry, String supplier, String qty, String cost) throws Exception {
        int q = Validation.quantity(qty);
        BigDecimal amount = Validation.money(cost, true);
        LocalDate h = LocalDate.parse(harvest), r = LocalDate.parse(receipt), e = LocalDate.parse(expiry);
        if (h.isAfter(r) || !r.isBefore(e) || !e.isAfter(LocalDate.now()) || !r.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Harvest date must not exceed receipt date; receipt date must not be in the future; expiry date must be in the future.");
        }
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                long sid = store(c, uid);
                if (Database.one(c, "SELECT v.id FROM variants v JOIN listings l ON l.id=v.listing_id WHERE v.id=? AND l.store_id=? AND v.status='ACTIVE'", variant, sid) == null) {
                    throw new IllegalArgumentException("The variant does not belong to your store.");
                }
                long bid = Database.insert(c, "INSERT batches(variant_id,batch_no,origin,harvest_date,receipt_date,expiry_date,supplier_name,received_qty,remaining_qty,unit_cost) VALUES(?,?,?,?,?,?,?,?,?,?)", variant, Validation.text(no, 30), Validation.text(origin, 100), java.sql.Date.valueOf(h), java.sql.Date.valueOf(r), java.sql.Date.valueOf(e), Validation.text(supplier, 150), q, q, amount);
                Database.update(c, "INSERT stock_movements(batch_id,actor_id,movement_type,quantity,idempotency_key) VALUES(?,?,'RECEIPT',?,?)", bid, uid, q, "receipt:" + bid);
                c.commit();
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    public void images(long uid, long listing, List<String> paths) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            String json = "[" + String.join(",", paths.stream().map(p -> "\"" + p + "\"").toList()) + "]";
            if (Database.update(c, "UPDATE listings SET image_urls=?,status='DRAFT',updated_at=SYSUTCDATETIME() WHERE id=? AND store_id=? AND status<>'RETIRED'", json, listing, store(c, uid)) != 1) {
                throw new IllegalArgumentException("Listing not found.");
            }
        }
    }

    public List<Map<String, Object>> images(long uid, long listing) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(c, "SELECT j.[key] AS position,j.value AS path FROM listings l CROSS APPLY OPENJSON(l.image_urls) j WHERE l.id=? AND l.store_id=? ORDER BY CONVERT(int,j.[key])", listing, store(c, uid));
        }
    }
}
