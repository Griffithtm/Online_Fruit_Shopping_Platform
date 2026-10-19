package model;

/**
 * Typed query or business result: Wallet.
 */
public class Wallet {

    private Integer pointsBalance;
    private String tier;
    private String benefits;
    private MembershipProgress membership;
    private WalletTotals totals;
    private java.util.List<Transaction> entries;

    public Wallet() {
    }

    public Integer getPointsBalance() {
        return pointsBalance;
    }

    public void setPointsBalance(Integer pointsBalance) {
        this.pointsBalance = pointsBalance;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getBenefits() {
        return benefits;
    }

    public void setBenefits(String benefits) {
        this.benefits = benefits;
    }

    public MembershipProgress getMembership() {
        return membership;
    }

    public void setMembership(MembershipProgress membership) {
        this.membership = membership;
    }

    public WalletTotals getTotals() {
        return totals;
    }

    public void setTotals(WalletTotals totals) {
        this.totals = totals;
    }

    public java.util.List<Transaction> getEntries() {
        return entries;
    }

    public void setEntries(java.util.List<Transaction> entries) {
        this.entries = entries;
    }
}
