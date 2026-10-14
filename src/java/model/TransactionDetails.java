package model;

/**
 * Typed query or business result: TransactionDetails.
 */
public class TransactionDetails extends Transaction {

    private String orderCode;

    public TransactionDetails() {
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }
}
