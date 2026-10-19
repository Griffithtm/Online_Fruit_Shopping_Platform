package service;

import model.*;

import config.DBConnect;
import dao.Database;
import java.sql.*;
import java.util.*;

public class CatalogService {

    public List<ServiceArea> deliveryAreas() throws Exception {
        try(Connection c=new DBConnect().getConnection()) {
            return Database.rows(ServiceArea.class, c, "SELECT name,TRY_CONVERT(decimal(15,2),JSON_VALUE(CASE WHEN ISJSON(value)=1 THEN value ELSE '{}' END,'$.delivery_fee')) AS delivery_fee FROM settings WHERE type='SERVICE_AREA' AND status='ACTIVE' ORDER BY priority,name");
        }
    }

    public List<Setting> categories() throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(Setting.class, c, "SELECT id,name FROM settings WHERE type='CATEGORY' AND status='ACTIVE' ORDER BY priority,name");
        }
    }

    public List<ProductOffer> products(String q, String category, String store, String min, String max, String rating, String sort, boolean promo) throws Exception {
        return products(q,category,store,min,max,rating,sort,promo,"in");
    }

    public List<ProductOffer> products(String q,String category,String store,String min,String max,String rating,String sort,boolean promo,String availability) throws Exception {
        String sql = "SELECT l.id AS listing_id,l.store_id,s.store_name,l.title,l.description,l.category_id,l.origin,v.id AS variant_id,v.option_name,v.unit,v.price,COALESCE(st.available_qty,0) AS available_qty,JSON_VALUE(l.image_urls,'$[0]') AS image,l.rating,l.created_at,s.rating AS seller_rating,COALESCE(pop.sold,0) AS sold FROM listings l JOIN stores s ON s.id=l.store_id JOIN users u ON u.id=s.owner_id JOIN variants v ON v.listing_id=l.id LEFT JOIN v_available_stock st ON st.variant_id=v.id OUTER APPLY(SELECT SUM(oi.quantity) sold FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id WHERE oi.variant_id=v.id AND so.status='DELIVERED') pop WHERE l.status='PUBLISHED' AND l.listing_type='PRODUCT' AND s.status='ACTIVE' AND u.status='ACTIVE' AND v.status='ACTIVE'";
        if (availability == null || availability.isBlank() || availability.equals("in")) sql += " AND COALESCE(st.available_qty,0)>0";
        else if (availability.equals("out")) sql += " AND COALESCE(st.available_qty,0)=0";
        else if (!availability.equals("all")) throw new IllegalArgumentException("Bộ lọc tình trạng hàng không hợp lệ.");
        if (min != null && !min.isBlank() && max != null && !max.isBlank() && Validation.money(min,true).compareTo(Validation.money(max,true))>0) throw new IllegalArgumentException("Giá tối thiểu không được lớn hơn giá tối đa.");
        List<Object> a = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            sql += " AND l.title LIKE ?";
            a.add("%" + q.trim() + "%");
        }
        if (category != null && !category.isBlank()) {
            sql += " AND l.category_id=?";
            a.add(Validation.id(category));
        }
        if (store != null && !store.isBlank()) {
            sql += " AND l.store_id=?";
            a.add(Validation.id(store));
        }
        if (min != null && !min.isBlank()) {
            sql += " AND v.price>=?";
            a.add(Validation.money(min, true));
        }
        if (max != null && !max.isBlank()) {
            sql += " AND v.price<=?";
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
                "v.price ASC";
            case "priceDesc" ->
                "v.price DESC";
            case "rating" ->
                "l.rating DESC";
            case "popular" ->
                "sold DESC";
            default ->
                "l.created_at DESC";
        } + ",v.id DESC";
        try (Connection c = new DBConnect().getConnection()) {
            List<ProductOffer> result=Database.rows(ProductOffer.class, c, sql, a.toArray());
            UnitPrice.decorate(result);
            if ("unitPrice".equals(sort)) result.sort(Comparator.comparing(x -> x.getUnitPriceKg(), Comparator.nullsLast(Comparator.naturalOrder())));
            return result;
        }
    }

    public ListingDetails product(long id) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.one(ListingDetails.class, c, "SELECT l.*,s.store_name,s.rating AS seller_rating,s.description AS store_description,JSON_VALUE(l.image_urls,'$[0]') AS image FROM listings l JOIN stores s ON s.id=l.store_id JOIN users u ON u.id=s.owner_id WHERE l.id=? AND l.status='PUBLISHED' AND s.status='ACTIVE' AND u.status='ACTIVE' AND EXISTS(SELECT 1 FROM variants v WHERE v.listing_id=l.id AND v.status='ACTIVE')", id);
        }
    }

    public List<VariantDetails> variants(long id) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(VariantDetails.class, c, "SELECT v.*,COALESCE(st.available_qty,0) AS available_qty FROM variants v LEFT JOIN v_available_stock st ON st.variant_id=v.id WHERE v.listing_id=? AND v.status='ACTIVE'", id);
        }
    }

    public List<Store> shops() throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(Store.class, c, "SELECT s.id,s.store_name,s.description,s.rating FROM stores s JOIN users u ON u.id=s.owner_id WHERE s.status='ACTIVE' AND u.status='ACTIVE'");
        }
    }

    public Store shop(long id) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.one(Store.class, c, "SELECT s.id,s.store_name,s.description,s.rating FROM stores s JOIN users u ON u.id=s.owner_id WHERE s.id=? AND s.status='ACTIVE' AND u.status='ACTIVE'", id);
        }
    }
}
