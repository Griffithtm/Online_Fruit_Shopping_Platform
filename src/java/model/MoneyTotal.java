package model;

/**
 * Typed query or business result: MoneyTotal.
 */
public class MoneyTotal {

    private java.math.BigDecimal amount;
    private java.math.BigDecimal total;

    public MoneyTotal() {
    }

    public java.math.BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(java.math.BigDecimal amount) {
        this.amount = amount;
    }

    public java.math.BigDecimal getTotal() {
        return total;
    }

    public void setTotal(java.math.BigDecimal total) {
        this.total = total;
    }
}
