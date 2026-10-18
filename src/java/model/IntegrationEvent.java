package model;

/**
 * Database entity: IntegrationEvent.
 */
public class IntegrationEvent {

    private Long id;
    private String provider;
    private String eventKey;
    private String eventType;
    private String payload;
    private String status;
    private java.sql.Timestamp receivedAt;
    private java.sql.Timestamp processedAt;
    private String errorMessage;

    public IntegrationEvent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.sql.Timestamp getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(java.sql.Timestamp receivedAt) {
        this.receivedAt = receivedAt;
    }

    public java.sql.Timestamp getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(java.sql.Timestamp processedAt) {
        this.processedAt = processedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
