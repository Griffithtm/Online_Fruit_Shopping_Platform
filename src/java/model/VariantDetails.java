package model;

/**
 * Typed query or business result: VariantDetails.
 */
public class VariantDetails extends Variant {

    private String title;
    private Long availableQty;

    public VariantDetails() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(Long availableQty) {
        this.availableQty = availableQty;
    }
}
