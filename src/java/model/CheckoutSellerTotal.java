package model;

/**
 * Typed query or business result: CheckoutSellerTotal.
 */
public class CheckoutSellerTotal {

    private java.math.BigDecimal subtotal;
    private java.math.BigDecimal discount;
    private String storeName;
    private Integer points;
    private java.math.BigDecimal pointsDiscount;
    private java.math.BigDecimal fee;

    public CheckoutSellerTotal() {
    }

    public java.math.BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(java.math.BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public java.math.BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(java.math.BigDecimal discount) {
        this.discount = discount;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public java.math.BigDecimal getPointsDiscount() {
        return pointsDiscount;
    }

    public void setPointsDiscount(java.math.BigDecimal pointsDiscount) {
        this.pointsDiscount = pointsDiscount;
    }

    public java.math.BigDecimal getFee() {
        return fee;
    }

    public void setFee(java.math.BigDecimal fee) {
        this.fee = fee;
    }
}
