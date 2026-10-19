package model;

/**
 * Database entity: SellerOrder.
 */
public class SellerOrder {

    private Long id;
    private Long orderId;
    private Long storeId;
    private String status;
    private java.math.BigDecimal subtotal;
    private java.math.BigDecimal commission;
    private String rejectionReason;
    private Long deliveryStaffId;
    private java.math.BigDecimal codAmount;
    private String coldChainNote;
    private Boolean evidenceRequired;
    private String evidenceUrl;
    private String failureReason;
    private java.sql.Timestamp assignedAt;
    private java.sql.Timestamp pickedUpAt;
    private java.sql.Timestamp deliveredAt;
    private java.sql.Timestamp failedAt;
    private String disputeStatus;
    private String disputeNote;
    private String disputeDecision;
    private String settlementStatus;
    private java.sql.Timestamp settledAt;
    private java.sql.Timestamp createdAt;
    private java.sql.Timestamp updatedAt;
    private java.math.BigDecimal deliveryFee;
    private java.math.BigDecimal discountAmount;
    private java.math.BigDecimal pointsDiscount;
    private java.math.BigDecimal commissionRate;
    private java.math.BigDecimal netPayable;
    private Integer pointsUsed;
    private String storeNameSnapshot;

    public SellerOrder() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.math.BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(java.math.BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public java.math.BigDecimal getCommission() {
        return commission;
    }

    public void setCommission(java.math.BigDecimal commission) {
        this.commission = commission;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Long getDeliveryStaffId() {
        return deliveryStaffId;
    }

    public void setDeliveryStaffId(Long deliveryStaffId) {
        this.deliveryStaffId = deliveryStaffId;
    }

    public java.math.BigDecimal getCodAmount() {
        return codAmount;
    }

    public void setCodAmount(java.math.BigDecimal codAmount) {
        this.codAmount = codAmount;
    }

    public String getColdChainNote() {
        return coldChainNote;
    }

    public void setColdChainNote(String coldChainNote) {
        this.coldChainNote = coldChainNote;
    }

    public Boolean getEvidenceRequired() {
        return evidenceRequired;
    }

    public void setEvidenceRequired(Boolean evidenceRequired) {
        this.evidenceRequired = evidenceRequired;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public java.sql.Timestamp getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(java.sql.Timestamp assignedAt) {
        this.assignedAt = assignedAt;
    }

    public java.sql.Timestamp getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(java.sql.Timestamp pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public java.sql.Timestamp getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(java.sql.Timestamp deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public java.sql.Timestamp getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(java.sql.Timestamp failedAt) {
        this.failedAt = failedAt;
    }

    public String getDisputeStatus() {
        return disputeStatus;
    }

    public void setDisputeStatus(String disputeStatus) {
        this.disputeStatus = disputeStatus;
    }

    public String getDisputeNote() {
        return disputeNote;
    }

    public void setDisputeNote(String disputeNote) {
        this.disputeNote = disputeNote;
    }

    public String getDisputeDecision() {
        return disputeDecision;
    }

    public void setDisputeDecision(String disputeDecision) {
        this.disputeDecision = disputeDecision;
    }

    public String getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(String settlementStatus) {
        this.settlementStatus = settlementStatus;
    }

    public java.sql.Timestamp getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(java.sql.Timestamp settledAt) {
        this.settledAt = settledAt;
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

    public java.math.BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(java.math.BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public java.math.BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(java.math.BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public java.math.BigDecimal getPointsDiscount() {
        return pointsDiscount;
    }

    public void setPointsDiscount(java.math.BigDecimal pointsDiscount) {
        this.pointsDiscount = pointsDiscount;
    }

    public java.math.BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(java.math.BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public java.math.BigDecimal getNetPayable() {
        return netPayable;
    }

    public void setNetPayable(java.math.BigDecimal netPayable) {
        this.netPayable = netPayable;
    }

    public Integer getPointsUsed() {
        return pointsUsed;
    }

    public void setPointsUsed(Integer pointsUsed) {
        this.pointsUsed = pointsUsed;
    }

    public String getStoreNameSnapshot() {
        return storeNameSnapshot;
    }

    public void setStoreNameSnapshot(String storeNameSnapshot) {
        this.storeNameSnapshot = storeNameSnapshot;
    }

    // ThanhNC 2.6a (BA-40, BA-38): delivery event times and COD collection time (UTC).
    private java.sql.Timestamp inTransitAt;
    private java.sql.Timestamp arrivingAt;
    private java.sql.Timestamp codCollectedAt;

    public java.sql.Timestamp getInTransitAt() {
        return inTransitAt;
    }

    public void setInTransitAt(java.sql.Timestamp inTransitAt) {
        this.inTransitAt = inTransitAt;
    }

    public java.sql.Timestamp getArrivingAt() {
        return arrivingAt;
    }

    public void setArrivingAt(java.sql.Timestamp arrivingAt) {
        this.arrivingAt = arrivingAt;
    }

    public java.sql.Timestamp getCodCollectedAt() {
        return codCollectedAt;
    }

    public void setCodCollectedAt(java.sql.Timestamp codCollectedAt) {
        this.codCollectedAt = codCollectedAt;
    }
}
