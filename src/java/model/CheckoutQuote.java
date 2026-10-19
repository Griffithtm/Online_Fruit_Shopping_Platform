package model;

/**
 * Typed query or business result: CheckoutQuote.
 */
public class CheckoutQuote {

    private Long cartId;
    private Address address;
    private java.util.List<CheckoutLine> lines;
    private java.math.BigDecimal subtotal;
    private java.math.BigDecimal fee;
    private java.math.BigDecimal discount;
    private java.math.BigDecimal pointsDiscount;
    private Integer points;
    private Long voucherId;
    private java.util.Map<Long, java.math.BigDecimal> discounts;
    private java.util.Map<Long, CheckoutSellerTotal> groups;
    private String method;
    private java.math.BigDecimal total;
    private Integer etaHours;
    private String fingerprint;

    public CheckoutQuote() {
    }

    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public java.util.List<CheckoutLine> getLines() {
        return lines;
    }

    public void setLines(java.util.List<CheckoutLine> lines) {
        this.lines = lines;
    }

    public java.math.BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(java.math.BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public java.math.BigDecimal getFee() {
        return fee;
    }

    public void setFee(java.math.BigDecimal fee) {
        this.fee = fee;
    }

    public java.math.BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(java.math.BigDecimal discount) {
        this.discount = discount;
    }

    public java.math.BigDecimal getPointsDiscount() {
        return pointsDiscount;
    }

    public void setPointsDiscount(java.math.BigDecimal pointsDiscount) {
        this.pointsDiscount = pointsDiscount;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public java.util.Map<Long, java.math.BigDecimal> getDiscounts() {
        return discounts;
    }

    public void setDiscounts(java.util.Map<Long, java.math.BigDecimal> discounts) {
        this.discounts = discounts;
    }

    public java.util.Map<Long, CheckoutSellerTotal> getGroups() {
        return groups;
    }

    public void setGroups(java.util.Map<Long, CheckoutSellerTotal> groups) {
        this.groups = groups;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public java.math.BigDecimal getTotal() {
        return total;
    }

    public void setTotal(java.math.BigDecimal total) {
        this.total = total;
    }

    public Integer getEtaHours() {
        return etaHours;
    }

    public void setEtaHours(Integer etaHours) {
        this.etaHours = etaHours;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    // ThanhNC 2.1 (BA-11, BA-12): fee of one seller order, the points limit and the usable balance shown at checkout.
    private java.math.BigDecimal areaFee;
    private Long maxPoints;
    private Long usablePoints;

    public java.math.BigDecimal getAreaFee() {
        return areaFee;
    }

    public void setAreaFee(java.math.BigDecimal areaFee) {
        this.areaFee = areaFee;
    }

    public Long getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(Long maxPoints) {
        this.maxPoints = maxPoints;
    }

    public Long getUsablePoints() {
        return usablePoints;
    }

    public void setUsablePoints(Long usablePoints) {
        this.usablePoints = usablePoints;
    }
}
