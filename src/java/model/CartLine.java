package model;

/**
 * Typed query or business result: CartLine.
 */
public class CartLine {

    private Long id;
    private Long variantId;
    private Integer quantity;
    private java.math.BigDecimal savedPrice;
    private java.math.BigDecimal unitPrice;
    private Integer purchasable;
    private java.math.BigDecimal lineTotal;
    private String title;
    private String optionName;
    private String storeName;
    private Long storeId;
    private Long availableQty;

    public CartLine() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public java.math.BigDecimal getSavedPrice() {
        return savedPrice;
    }

    public void setSavedPrice(java.math.BigDecimal savedPrice) {
        this.savedPrice = savedPrice;
    }

    public java.math.BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(java.math.BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getPurchasable() {
        return purchasable;
    }

    public void setPurchasable(Integer purchasable) {
        this.purchasable = purchasable;
    }

    public java.math.BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(java.math.BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOptionName() {
        return optionName;
    }

    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(Long availableQty) {
        this.availableQty = availableQty;
    }
}
