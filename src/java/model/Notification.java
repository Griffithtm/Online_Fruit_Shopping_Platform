package model;

/**
 * Database entity: Notification.
 */
public class Notification {

    private Long id;
    private Long userId;
    private String eventType;
    private String title;
    private String content;
    private String linkUrl;
    private Boolean sendEmail;
    private String emailStatus;
    private Integer emailRetryCount;
    private java.sql.Timestamp readAt;
    private java.sql.Timestamp createdAt;
    private String emailProviderRef;
    private java.sql.Timestamp nextRetryAt;
    private String dedupKey;

    public Notification() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public Boolean getSendEmail() {
        return sendEmail;
    }

    public void setSendEmail(Boolean sendEmail) {
        this.sendEmail = sendEmail;
    }

    public String getEmailStatus() {
        return emailStatus;
    }

    public void setEmailStatus(String emailStatus) {
        this.emailStatus = emailStatus;
    }

    public Integer getEmailRetryCount() {
        return emailRetryCount;
    }

    public void setEmailRetryCount(Integer emailRetryCount) {
        this.emailRetryCount = emailRetryCount;
    }

    public java.sql.Timestamp getReadAt() {
        return readAt;
    }

    public void setReadAt(java.sql.Timestamp readAt) {
        this.readAt = readAt;
    }

    public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.sql.Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getEmailProviderRef() {
        return emailProviderRef;
    }

    public void setEmailProviderRef(String emailProviderRef) {
        this.emailProviderRef = emailProviderRef;
    }

    public java.sql.Timestamp getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(java.sql.Timestamp nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public void setDedupKey(String dedupKey) {
        this.dedupKey = dedupKey;
    }
}
