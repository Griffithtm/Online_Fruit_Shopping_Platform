package model;

import java.math.BigDecimal;

public class PriceVariant {

    private int id;
    private String productName;
    private String variantName;
    private BigDecimal price;

    public PriceVariant(int id, String productName, String variantName, BigDecimal price) {
        this.id = id;
        this.productName = productName;
        this.variantName = variantName;
        this.price = price;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public String getVariantName() {
        return variantName;
    }

    public BigDecimal getPrice() {
        return price;
    }

}
