package model;

/**
 * Typed query or business result: StockProduct.
 */
public class StockProduct {

    private java.math.BigDecimal price;
    private String listingStatus;
    private String variantStatus;
    private String storeStatus;
    private String ownerStatus;
    private String listingType;

    public StockProduct() {
    }

    public java.math.BigDecimal getPrice() {
        return price;
    }

    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }

    public String getListingStatus() {
        return listingStatus;
    }

    public void setListingStatus(String listingStatus) {
        this.listingStatus = listingStatus;
    }

    public String getVariantStatus() {
        return variantStatus;
    }

    public void setVariantStatus(String variantStatus) {
        this.variantStatus = variantStatus;
    }

    public String getStoreStatus() {
        return storeStatus;
    }

    public void setStoreStatus(String storeStatus) {
        this.storeStatus = storeStatus;
    }

    public String getOwnerStatus() {
        return ownerStatus;
    }

    public void setOwnerStatus(String ownerStatus) {
        this.ownerStatus = ownerStatus;
    }

    public String getListingType() {
        return listingType;
    }

    public void setListingType(String listingType) {
        this.listingType = listingType;
    }
}
