package model;

/**
 * Typed query or business result: PromotionDetails.
 */
public class PromotionDetails extends Promotion {

    private Integer redemptions;
    private java.util.List<RedemptionDetails> history;

    public PromotionDetails() {
    }

    public Integer getRedemptions() {
        return redemptions;
    }

    public void setRedemptions(Integer redemptions) {
        this.redemptions = redemptions;
    }

    public java.util.List<RedemptionDetails> getHistory() {
        return history;
    }

    public void setHistory(java.util.List<RedemptionDetails> history) {
        this.history = history;
    }
}
