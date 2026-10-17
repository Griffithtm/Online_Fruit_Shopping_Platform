package model;

/**
 * Typed query or business result: MembershipRule.
 */
public class MembershipRule extends Setting {

    private java.math.BigDecimal threshold;

    public MembershipRule() {
    }

    public java.math.BigDecimal getThreshold() {
        return threshold;
    }

    public void setThreshold(java.math.BigDecimal threshold) {
        this.threshold = threshold;
    }
}
