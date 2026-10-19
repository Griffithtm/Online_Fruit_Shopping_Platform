package model;

/** Database entity: Store. */
public class Store {
    private Long id;
    private Long ownerId;
    private String storeName;
    private String description;
    private String businessInfo;
    private Long pickupAddressId;
    private Long sellerTierId;
    private java.math.BigDecimal rating;
    private String status;
    private java.sql.Timestamp createdAt;
    private java.sql.Timestamp updatedAt;

    public Store() {}

    public Long getId() {
        return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBusinessInfo() { return businessInfo; }
    public void setBusinessInfo(String businessInfo) { this.businessInfo = businessInfo; }

    public Long getPickupAddressId() { return pickupAddressId; }
    public void setPickupAddressId(Long pickupAddressId) { this.pickupAddressId = pickupAddressId; }

    public Long getSellerTierId() { return sellerTierId; }
    public void setSellerTierId(Long sellerTierId) { this.sellerTierId = sellerTierId; }

    public java.math.BigDecimal getRating() { return rating; }
    public void setRating(java.math.BigDecimal rating) { this.rating = rating; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public java.sql.Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.sql.Timestamp createdAt) { this.createdAt = createdAt; }

    public java.sql.Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.sql.Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
