package model;

/**
 * Typed query or business result: OrderSummary.
 */
public class OrderSummary extends Order {

    private java.math.BigDecimal currentPayable;
    private String paymentStatus;
    private Integer cancellable;

    public OrderSummary() {
    }

    public java.math.BigDecimal getCurrentPayable() {
        return currentPayable;
    }

    public void setCurrentPayable(java.math.BigDecimal currentPayable) {
        this.currentPayable = currentPayable;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Integer getCancellable() {
        return cancellable;
    }

    public void setCancellable(Integer cancellable) {
        this.cancellable = cancellable;
    }
}
