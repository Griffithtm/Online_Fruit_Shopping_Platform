package model;

/**
 * Typed query or business result: MembershipProgress.
 */
public class MembershipProgress {

    private Integer periodDays;
    private java.math.BigDecimal qualifyingSpend;
    private String warning;
    private Long tierId;
    private String nextTier;
    private java.math.BigDecimal remainingSpend;

    public MembershipProgress() {
    }

    public Integer getPeriodDays() {
        return periodDays;
    }

    public void setPeriodDays(Integer periodDays) {
        this.periodDays = periodDays;
    }

    public java.math.BigDecimal getQualifyingSpend() {
        return qualifyingSpend;
    }

    public void setQualifyingSpend(java.math.BigDecimal qualifyingSpend) {
        this.qualifyingSpend = qualifyingSpend;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }

    public Long getTierId() {
        return tierId;
    }

    public void setTierId(Long tierId) {
        this.tierId = tierId;
    }

    public String getNextTier() {
        return nextTier;
    }

    public void setNextTier(String nextTier) {
        this.nextTier = nextTier;
    }

    public java.math.BigDecimal getRemainingSpend() {
        return remainingSpend;
    }

    public void setRemainingSpend(java.math.BigDecimal remainingSpend) {
        this.remainingSpend = remainingSpend;
    }
}
