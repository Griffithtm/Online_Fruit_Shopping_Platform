package model;

/**
 * Typed query or business result: RedemptionDetails.
 */
public class RedemptionDetails extends PromotionRedemption {

    private String orderCode;

    public RedemptionDetails() {
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }
}
