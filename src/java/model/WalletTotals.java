package model;

/**
 * Typed query or business result: WalletTotals.
 */
public class WalletTotals {

    private Integer pending;
    private Integer expired;

    public WalletTotals() {
    }

    public Integer getPending() {
        return pending;
    }

    public void setPending(Integer pending) {
        this.pending = pending;
    }

    public Integer getExpired() {
        return expired;
    }

    public void setExpired(Integer expired) {
        this.expired = expired;
    }
}
