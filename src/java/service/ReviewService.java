package service;

import config.DBConnect;
import dao.Database;
import model.Review;
import model.ReviewReport;
import java.sql.*;
import java.util.*;

/** Verified customer reviews, customer reports and simple admin moderation. */
public class ReviewService {
    public List<Map<String,Object>> customerItems(long customerId) throws Exception {
        String sql = "SELECT oi.id AS item_id,l.title,v.option_name,v.unit,oi.quantity,so.delivered_at,"
            + "r.id AS review_id,r.product_rating,r.seller_rating,r.comment,r.moderation_status,r.created_at "
            + "FROM orders o JOIN seller_orders so ON so.order_id=o.id JOIN order_items oi ON oi.seller_order_id=so.id "
            + "JOIN variants v ON v.id=oi.variant_id JOIN listings l ON l.id=v.listing_id "
            + "LEFT JOIN reviews r ON r.order_item_id=oi.id AND r.customer_id=o.customer_id "
            + "WHERE o.customer_id=? AND so.status='DELIVERED' ORDER BY so.delivered_at DESC,oi.id DESC";
        try(Connection c=new DBConnect().getConnection(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setLong(1,customerId); try(ResultSet rs=p.executeQuery()){return rows(rs);}
        }
    }

    public List<Map<String,Object>> publicReviews(long listingId) throws Exception {
        String sql="SELECT r.id,r.product_rating,r.seller_rating,r.comment,r.created_at,u.full_name "
            +"FROM reviews r JOIN order_items oi ON oi.id=r.order_item_id JOIN variants v ON v.id=oi.variant_id "
            +"JOIN users u ON u.id=r.customer_id JOIN seller_orders so ON so.id=oi.seller_order_id "
            +"WHERE v.listing_id=? AND so.status='DELIVERED' AND r.moderation_status IN ('PUBLISHED','REPORTED','KEPT') "
            +"ORDER BY r.created_at DESC";
        try(Connection c=new DBConnect().getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,listingId);try(ResultSet rs=p.executeQuery()){return rows(rs);}}
    }

    public void save(long customerId,long itemId,int productRating,int sellerRating,String comment) throws Exception {
        if(productRating<1||productRating>5||sellerRating<1||sellerRating>5) throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5.");
        if(comment!=null && comment.trim().length()>2000) throw new IllegalArgumentException("Nội dung đánh giá tối đa 2.000 ký tự.");
        try(Connection c=new DBConnect().getConnection()){
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE); c.setAutoCommit(false);
            try {
                String eligibility="SELECT oi.id AS id FROM order_items oi WITH(UPDLOCK,HOLDLOCK) JOIN seller_orders so ON so.id=oi.seller_order_id "
                    +"JOIN orders o ON o.id=so.order_id WHERE oi.id=? AND o.customer_id=? AND so.status='DELIVERED'";
                if(Database.one(Review.class,c,eligibility,itemId,customerId)==null) throw new IllegalArgumentException("Bạn chỉ có thể đánh giá sản phẩm thuộc đơn hàng đã giao.");
                Review old=Database.one(Review.class,c,"SELECT * FROM reviews WITH(UPDLOCK,HOLDLOCK) WHERE order_item_id=?",itemId);
                if(old==null){
                    Database.update(c,"INSERT INTO reviews(order_item_id,customer_id,product_rating,seller_rating,comment,moderation_status) VALUES(?,?,?,?,?,'PUBLISHED')",itemId,customerId,productRating,sellerRating,blankToNull(comment));
                    audit(c,customerId,itemId,"REVIEW_CREATED","Khách hàng đã gửi đánh giá.");
                } else {
                    if(old.getCustomerId()==null||old.getCustomerId()!=customerId) throw new IllegalArgumentException("Bạn không có quyền sửa đánh giá này.");
                    if(Set.of("HIDDEN","REMOVED").contains(old.getModerationStatus())) throw new IllegalArgumentException("Đánh giá đang bị ẩn hoặc đã gỡ, không thể chỉnh sửa.");
                    Database.update(c,"UPDATE reviews SET product_rating=?,seller_rating=?,comment=?,updated_at=SYSUTCDATETIME() WHERE id=? AND customer_id=?",productRating,sellerRating,blankToNull(comment),old.getId(),customerId);
                    audit(c,customerId,old.getId(),"REVIEW_UPDATED","Khách hàng đã chỉnh sửa đánh giá.");
                }
                recalculate(c,itemId); c.commit();
            } catch(Exception e){c.rollback();throw e;}
        }
    }

    public void delete(long customerId,long reviewId) throws Exception {
        try(Connection c=new DBConnect().getConnection()){
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
            try{
                Review r=Database.one(Review.class,c,"SELECT * FROM reviews WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND customer_id=?",reviewId,customerId);
                if(r==null)throw new IllegalArgumentException("Không tìm thấy đánh giá của bạn.");
                long itemId=r.getOrderItemId();
                audit(c,customerId,reviewId,"REVIEW_DELETED","Khách hàng đã xóa đánh giá.");
                Database.update(c,"DELETE FROM reviews WHERE id=? AND customer_id=?",reviewId,customerId);
                recalculate(c,itemId);c.commit();
            }catch(Exception e){c.rollback();throw e;}
        }
    }

    public void report(long customerId,long reviewId,String reason,String description) throws Exception {
        if(!Set.of("SPAM","OFFENSIVE","MISLEADING","OTHER").contains(reason))throw new IllegalArgumentException("Lý do báo cáo không hợp lệ.");
        if(description!=null&&description.trim().length()>500)throw new IllegalArgumentException("Mô tả tối đa 500 ký tự.");
        try(Connection c=new DBConnect().getConnection()){
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
            try{
                Review review=Database.one(Review.class,c,"SELECT * FROM reviews WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND moderation_status NOT IN ('HIDDEN','REMOVED')",reviewId);
                if(review==null)throw new IllegalArgumentException("Đánh giá không còn khả dụng để báo cáo.");
                if(review.getCustomerId()!=null&&review.getCustomerId()==customerId)throw new IllegalArgumentException("Bạn không thể báo cáo đánh giá của chính mình.");
                ReviewReport exists=Database.one(ReviewReport.class,c,"SELECT * FROM review_reports WITH(UPDLOCK,HOLDLOCK) WHERE review_id=? AND reporter_id=?",reviewId,customerId);
                if(exists!=null)throw new IllegalArgumentException("Bạn đã báo cáo đánh giá này.");
                Database.update(c,"INSERT INTO review_reports(review_id,reporter_id,reason,description,status) VALUES(?,?,?,?,'OPEN')",reviewId,customerId,reason,blankToNull(description));
                Database.update(c,"UPDATE reviews SET moderation_status='REPORTED',report_count=(SELECT COUNT(*) FROM review_reports WHERE review_id=? AND status='OPEN'),updated_at=SYSUTCDATETIME() WHERE id=?",reviewId,reviewId);
                audit(c,customerId,reviewId,"REVIEW_REPORTED","Báo cáo: "+reason);
                c.commit();
            }catch(Exception e){c.rollback();throw e;}
        }
    }

    public List<Map<String,Object>> reportQueue() throws Exception {
        String sql="SELECT rr.id AS report_id,rr.review_id,rr.reporter_id,rr.reason,rr.description,rr.created_at AS report_created,"
            +"r.comment,r.product_rating,r.seller_rating,r.moderation_status,u.full_name AS reporter_name,"
            +"author.full_name AS author_name,l.title,COUNT(*) OVER(PARTITION BY rr.review_id) AS open_count "
            +"FROM review_reports rr JOIN reviews r ON r.id=rr.review_id JOIN users u ON u.id=rr.reporter_id "
            +"JOIN users author ON author.id=r.customer_id JOIN order_items oi ON oi.id=r.order_item_id "
            +"JOIN variants v ON v.id=oi.variant_id JOIN listings l ON l.id=v.listing_id WHERE rr.status='OPEN' ORDER BY rr.created_at ASC";
        try(Connection c=new DBConnect().getConnection();PreparedStatement p=c.prepareStatement(sql);ResultSet rs=p.executeQuery()){return rows(rs);}
    }

    public void moderate(long adminId,long reviewId,String decision,String note) throws Exception {
        if(!Set.of("KEEP","HIDE","REMOVE").contains(decision))throw new IllegalArgumentException("Quyết định kiểm duyệt không hợp lệ.");
        if(note!=null&&note.trim().length()>500)throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự.");
        try(Connection c=new DBConnect().getConnection()){
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
            try{
                if(Database.one(model.User.class,c,"SELECT id FROM users WHERE id=? AND role='ADMIN' AND status='ACTIVE'",adminId)==null)throw new IllegalArgumentException("Bạn không có quyền quản trị.");
                Review r=Database.one(Review.class,c,"SELECT * FROM reviews WITH(UPDLOCK,HOLDLOCK) WHERE id=?",reviewId);
                if(r==null)throw new IllegalArgumentException("Không tìm thấy đánh giá.");
                String status=switch(decision){case "KEEP"->"KEPT";case "HIDE"->"HIDDEN";default->"REMOVED";};
                Database.update(c,"UPDATE reviews SET moderation_status=?,updated_at=SYSUTCDATETIME() WHERE id=?",status,reviewId);
                Database.update(c,"UPDATE review_reports SET status='RESOLVED' WHERE review_id=? AND status='OPEN'",reviewId);
                Database.update(c,"UPDATE reviews SET report_count=0 WHERE id=?",reviewId);
                audit(c,adminId,reviewId,"REVIEW_MODERATED",decision+(note==null||note.isBlank()?"":" - "+note.trim()));
                recalculate(c,r.getOrderItemId());c.commit();
            }catch(Exception e){c.rollback();throw e;}
        }
    }

    private void recalculate(Connection c,long itemId)throws Exception{
        Database.update(c,"UPDATE listings SET rating=COALESCE((SELECT AVG(CAST(r.product_rating AS DECIMAL(5,2))) FROM reviews r JOIN order_items oi ON oi.id=r.order_item_id JOIN variants v ON v.id=oi.variant_id JOIN seller_orders so ON so.id=oi.seller_order_id WHERE v.listing_id=listings.id AND so.status='DELIVERED' AND r.moderation_status IN ('PUBLISHED','REPORTED','KEPT')),0) WHERE id=(SELECT v.listing_id FROM order_items oi JOIN variants v ON v.id=oi.variant_id WHERE oi.id=?)",itemId);
        Database.update(c,"UPDATE stores SET rating=COALESCE((SELECT AVG(CAST(r.seller_rating AS DECIMAL(5,2))) FROM reviews r JOIN order_items oi ON oi.id=r.order_item_id JOIN seller_orders so ON so.id=oi.seller_order_id WHERE so.store_id=stores.id AND so.status='DELIVERED' AND r.moderation_status IN ('PUBLISHED','REPORTED','KEPT')),0) WHERE id=(SELECT so.store_id FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id WHERE oi.id=?)",itemId);
    }
    private void audit(Connection c,long actor,long entity,String action,String note)throws Exception{
        Database.update(c,"INSERT INTO audit_logs(actor_id,entity_type,entity_id,action,note) VALUES(?,'REVIEW',?,?,?)",actor,entity,action,note);
    }
    private static String blankToNull(String s){return s==null||s.trim().isEmpty()?null:s.trim();}
    private static List<Map<String,Object>> rows(ResultSet rs)throws Exception{
        List<Map<String,Object>> out=new ArrayList<>();ResultSetMetaData md=rs.getMetaData();int n=md.getColumnCount();
        while(rs.next()){Map<String,Object> row=new LinkedHashMap<>();for(int i=1;i<=n;i++)row.put(md.getColumnLabel(i),rs.getObject(i));out.add(row);}return out;
    }
}
