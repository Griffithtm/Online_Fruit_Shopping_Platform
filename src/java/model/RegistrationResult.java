package model;

/**
 * Identifiers returned by customer or seller registration.
 */
public class RegistrationResult {

    private Long userId;
    private Long storeId;

    public RegistrationResult() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
}
