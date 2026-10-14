package model;

/**
 * Typed query or business result: ListingDetails.
 */
public class ListingDetails extends Listing {

    private String storeName;
    private java.math.BigDecimal sellerRating;
    private String storeDescription;
    private String image;

    public ListingDetails() {
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public java.math.BigDecimal getSellerRating() {
        return sellerRating;
    }

    public void setSellerRating(java.math.BigDecimal sellerRating) {
        this.sellerRating = sellerRating;
    }

    public String getStoreDescription() {
        return storeDescription;
    }

    public void setStoreDescription(String storeDescription) {
        this.storeDescription = storeDescription;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
