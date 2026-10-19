package model;

/**
 * Typed query or business result: ServiceArea.
 */
public class ServiceArea {

    private String name;
    private java.math.BigDecimal deliveryFee;
    private java.math.BigDecimal fee;

    public ServiceArea() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public java.math.BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(java.math.BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public java.math.BigDecimal getFee() {
        return fee;
    }

    public void setFee(java.math.BigDecimal fee) {
        this.fee = fee;
    }

    // ThanhNC 2.1 (BA-11): optional "eta_hours" of the service area JSON; null means the delivery_eta_hours policy.
    private Integer etaHours;

    public Integer getEtaHours() {
        return etaHours;
    }

    public void setEtaHours(Integer etaHours) {
        this.etaHours = etaHours;
    }
}
