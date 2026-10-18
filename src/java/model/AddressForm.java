package model;

/** Submitted address fields retained when validation fails. */
public class AddressForm extends Address {
    private Boolean otherCity;
    public Boolean getOtherCity() { return otherCity; }
    public void setOtherCity(Boolean otherCity) { this.otherCity = otherCity; }
}
