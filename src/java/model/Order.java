package model;

/**
 * Database entity: Order.
 */
public class Order {
    private Long deliveryAddressId;
    public Long getDeliveryAddressId() { return deliveryAddressId; }
    public void setDeliveryAddressId(Long value) { deliveryAddressId = value; }

    private Long id;
    private String orderCode;
    private Long customerId;
    private String status;
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;
    private Long voucherId;
    private Integer pointsUsed;
    private java.math.BigDecimal itemTotal;
    private java.math.BigDecimal discountAmount;
    private java.math.BigDecimal deliveryFee;
    private java.math.BigDecimal payableAmount;
    private String paymentMethod;
    private java.sql.Timestamp estimatedDeliveryAt;
    private Long subscriptionId;
    private java.sql.Timestamp placedAt;
    private java.sql.Timestamp createdAt;
    private java.sql.Timestamp updatedAt;
    private java.sql.Date subscriptionOccurrence;
    private java.math.BigDecimal pointsDiscount;
    private String checkoutKey;
    private java.sql.Timestamp paymentExpiresAt;

    public Order() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public Integer getPointsUsed() {
        return pointsUsed;
    }

    public void setPointsUsed(Integer pointsUsed) {
        this.pointsUsed = pointsUsed;
    }

    public java.math.BigDecimal getItemTotal() {
        return itemTotal;
    }

    public void setItemTotal(java.math.BigDecimal itemTotal) {
        this.itemTotal = itemTotal;
    }

    public java.math.BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(java.math.BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public java.math.BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(java.math.BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public java.math.BigDecimal getPayableAmount() {
        return payableAmount;
    }

    public void setPayableAmount(java.math.BigDecimal payableAmount) {
        this.payableAmount = payableAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public java.sql.Timestamp getEstimatedDeliveryAt() {
        return estimatedDeliveryAt;
    }

    public void setEstimatedDeliveryAt(java.sql.Timestamp estimatedDeliveryAt) {
        this.estimatedDeliveryAt = estimatedDeliveryAt;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public java.sql.Timestamp getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(java.sql.Timestamp placedAt) {
        this.placedAt = placedAt;
    }

    public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.sql.Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public java.sql.Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.sql.Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public java.sql.Date getSubscriptionOccurrence() {
        return subscriptionOccurrence;
    }

    public void setSubscriptionOccurrence(java.sql.Date subscriptionOccurrence) {
        this.subscriptionOccurrence = subscriptionOccurrence;
    }

    public java.math.BigDecimal getPointsDiscount() {
        return pointsDiscount;
    }

    public void setPointsDiscount(java.math.BigDecimal pointsDiscount) {
        this.pointsDiscount = pointsDiscount;
    }

    public String getCheckoutKey() {
        return checkoutKey;
    }

    public void setCheckoutKey(String checkoutKey) {
        this.checkoutKey = checkoutKey;
    }

    public java.sql.Timestamp getPaymentExpiresAt() {
        return paymentExpiresAt;
    }

    public void setPaymentExpiresAt(java.sql.Timestamp paymentExpiresAt) {
        this.paymentExpiresAt = paymentExpiresAt;
    }

    // ThanhNC 2.2 (BA-15): cancellation deadline snapshotted at placement (UTC).
    private java.sql.Timestamp cancelDeadlineAt;

    public java.sql.Timestamp getCancelDeadlineAt() {
        return cancelDeadlineAt;
    }

    public void setCancelDeadlineAt(java.sql.Timestamp cancelDeadlineAt) {
        this.cancelDeadlineAt = cancelDeadlineAt;
    }
}
