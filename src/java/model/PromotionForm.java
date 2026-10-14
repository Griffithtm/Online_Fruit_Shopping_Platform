package model;

/**
 * Typed query or business result: PromotionForm.
 */
public class PromotionForm {

    private String id;
    private String action;
    private String state;
    private String name;
    private String discountType;
    private String value;
    private String budget;
    private String minimum;
    private String limit;
    private String perCustomer;
    private String start;
    private String end;
    private String code;
    private String listings;

    public PromotionForm() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public String getMinimum() {
        return minimum;
    }

    public void setMinimum(String minimum) {
        this.minimum = minimum;
    }

    public String getLimit() {
        return limit;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    public String getPerCustomer() {
        return perCustomer;
    }

    public void setPerCustomer(String perCustomer) {
        this.perCustomer = perCustomer;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getListings() {
        return listings;
    }

    public void setListings(String listings) {
        this.listings = listings;
    }
}
