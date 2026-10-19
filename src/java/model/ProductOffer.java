package model;

/**
 * Database entity: ProductOffer.
 */
public class ProductOffer {

    private Long listingId;
    private Long storeId;
    private String storeName;
    private String title;
    private String description;
    private String imageUrls;
    private Long categoryId;
    private String origin;
    private Long variantId;
    private String optionName;
    private String unit;
    private java.math.BigDecimal price;
    private Long availableQty;
    private String image;
    private java.math.BigDecimal rating;
    private java.sql.Timestamp createdAt;
    private java.math.BigDecimal sellerRating;
    private Integer sold;
    private java.math.BigDecimal unitPriceKg;

    public ProductOffer() {
    }

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
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

    public String getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public String getOptionName() {
        return optionName;
    }

    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public java.math.BigDecimal getPrice() {
        return price;
    }

    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }

    public Long getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(Long availableQty) {
        this.availableQty = availableQty;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public java.math.BigDecimal getRating() {
        return rating;
    }

    public void setRating(java.math.BigDecimal rating) {
        this.rating = rating;
    }

    public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.sql.Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public java.math.BigDecimal getSellerRating() {
        return sellerRating;
    }

    public void setSellerRating(java.math.BigDecimal sellerRating) {
        this.sellerRating = sellerRating;
    }

    public Integer getSold() {
        return sold;
    }

    public void setSold(Integer sold) {
        this.sold = sold;
    }

    public java.math.BigDecimal getUnitPriceKg() {
        return unitPriceKg;
    }

    public void setUnitPriceKg(java.math.BigDecimal unitPriceKg) {
        this.unitPriceKg = unitPriceKg;
    }
}
