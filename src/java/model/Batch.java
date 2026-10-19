package model;

/**
 * Database entity: Batch.
 */
public class Batch {

    private Long id;
    private Long variantId;
    private String batchNo;
    private String origin;
    private java.sql.Date harvestDate;
    private java.sql.Date receiptDate;
    private java.sql.Date expiryDate;
    private String supplierName;
    private Integer receivedQty;
    private Integer remainingQty;
    private Integer reservedQty;
    private java.math.BigDecimal unitCost;
    private java.math.BigDecimal totalCost;
    private String status;
    private java.sql.Timestamp createdAt;

    public Batch() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public java.sql.Date getHarvestDate() {
        return harvestDate;
    }

    public void setHarvestDate(java.sql.Date harvestDate) {
        this.harvestDate = harvestDate;
    }

    public java.sql.Date getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(java.sql.Date receiptDate) {
        this.receiptDate = receiptDate;
    }

    public java.sql.Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(java.sql.Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Integer getReceivedQty() {
        return receivedQty;
    }

    public void setReceivedQty(Integer receivedQty) {
        this.receivedQty = receivedQty;
    }

    public Integer getRemainingQty() {
        return remainingQty;
    }

    public void setRemainingQty(Integer remainingQty) {
        this.remainingQty = remainingQty;
    }

    public Integer getReservedQty() {
        return reservedQty;
    }

    public void setReservedQty(Integer reservedQty) {
        this.reservedQty = reservedQty;
    }

    public java.math.BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(java.math.BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public java.math.BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(java.math.BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.sql.Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
