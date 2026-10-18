package model;

/**
 * Database entity: AvailableStock.
 */
public class AvailableStock {

    private Long variantId;
    private Long availableQty;

    public AvailableStock() {
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Long getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(Long availableQty) {
        this.availableQty = availableQty;
    }
}
