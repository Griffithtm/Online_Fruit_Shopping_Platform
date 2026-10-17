package model;

/**
 * Database entity: Promotion.
 */
public class Promotion {

    private Long id;
    private String type;
    private Long storeId;
    private String name;
    private String code;
    private String discountType;
    private java.math.BigDecimal discountValue;
    private java.math.BigDecimal maxDiscount;
    private java.math.BigDecimal minSpend;
    private java.math.BigDecimal budgetLimit;
    private java.math.BigDecimal budgetUsed;
    private Integer usageLimit;
    private Integer perCustomerLimit;
    private Integer usedCount;
    private java.sql.Timestamp startAt;
    private java.sql.Timestamp endAt;
    private String status;
    private java.sql.Timestamp createdAt;
    private java.sql.Timestamp updatedAt;

    public Promotion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public java.math.BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(java.math.BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public java.math.BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(java.math.BigDecimal maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public java.math.BigDecimal getMinSpend() {
        return minSpend;
    }

    public void setMinSpend(java.math.BigDecimal minSpend) {
        this.minSpend = minSpend;
    }

    public java.math.BigDecimal getBudgetLimit() {
        return budgetLimit;
    }

    public void setBudgetLimit(java.math.BigDecimal budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    public java.math.BigDecimal getBudgetUsed() {
        return budgetUsed;
    }

    public void setBudgetUsed(java.math.BigDecimal budgetUsed) {
        this.budgetUsed = budgetUsed;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public Integer getPerCustomerLimit() {
        return perCustomerLimit;
    }

    public void setPerCustomerLimit(Integer perCustomerLimit) {
        this.perCustomerLimit = perCustomerLimit;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    public java.sql.Timestamp getStartAt() {
        return startAt;
    }

    public void setStartAt(java.sql.Timestamp startAt) {
        this.startAt = startAt;
    }

    public java.sql.Timestamp getEndAt() {
        return endAt;
    }

    public void setEndAt(java.sql.Timestamp endAt) {
        this.endAt = endAt;
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

    public java.sql.Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.sql.Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
