package model;

/**
 * Database entity: Listing.
 */
public class Listing {

    private Long id;
    private Long storeId;
    private Long categoryId;
    private Long campaignId;
    private String listingType;
    private String title;
    private String description;
    private String origin;
    private String harvestInfo;
    private String imageUrls;
    private String planFrequency;
    private String planTerms;
    private Boolean enrollmentOpen;
    private java.math.BigDecimal rating;
    private String status;
    private String rejectionReason;
    private java.sql.Timestamp createdAt;
    private java.sql.Timestamp updatedAt;

    public Listing() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public String getListingType() {
        return listingType;
    }

    public void setListingType(String listingType) {
        this.listingType = listingType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getHarvestInfo() {
        return harvestInfo;
    }

    public void setHarvestInfo(String harvestInfo) {
        this.harvestInfo = harvestInfo;
    }

    public String getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getPlanFrequency() {
        return planFrequency;
    }

    public void setPlanFrequency(String planFrequency) {
        this.planFrequency = planFrequency;
    }

    public String getPlanTerms() {
        return planTerms;
    }

    public void setPlanTerms(String planTerms) {
        this.planTerms = planTerms;
    }

    public Boolean getEnrollmentOpen() {
        return enrollmentOpen;
    }

    public void setEnrollmentOpen(Boolean enrollmentOpen) {
        this.enrollmentOpen = enrollmentOpen;
    }

    public java.math.BigDecimal getRating() {
        return rating;
    }

    public void setRating(java.math.BigDecimal rating) {
        this.rating = rating;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
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
