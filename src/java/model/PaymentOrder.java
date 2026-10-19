package model;

/**
 * Typed query or business result: PaymentOrder.
 */
public class PaymentOrder extends Order {

    private Integer expired;

    public PaymentOrder() {
    }

    public Integer getExpired() {
        return expired;
    }

    public void setExpired(Integer expired) {
        this.expired = expired;
    }
}
