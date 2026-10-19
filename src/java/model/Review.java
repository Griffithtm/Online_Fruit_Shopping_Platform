package model;

/**
 * Database entity: Review.
 */
public class Review {

    private Long id;
    private Long orderItemId;
    private Long customerId;
    private Short productRating;
    private Short sellerRating;
    private String comment;
    private Integer reportCount;
    private String moderationStatus;
    private java.sql.Timestamp createdAt;
    private java.sql.Timestamp updatedAt;

    public Review() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Short getProductRating() {
        return productRating;
    }

    public void setProductRating(Short productRating) {
        this.productRating = productRating;
    }

    public Short getSellerRating() {
        return sellerRating;
    }

    public void setSellerRating(Short sellerRating) {
        this.sellerRating = sellerRating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getReportCount() {
        return reportCount;
    }

    public void setReportCount(Integer reportCount) {
        this.reportCount = reportCount;
    }

    public String getModerationStatus() {
        return moderationStatus;
    }

    public void setModerationStatus(String moderationStatus) {
        this.moderationStatus = moderationStatus;
    }

    public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.sql.Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public java.sql.Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.sql.Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
