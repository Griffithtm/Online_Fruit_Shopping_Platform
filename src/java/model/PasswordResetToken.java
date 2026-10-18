package model;

/**
 * Database entity: PasswordResetToken.
 */
public class PasswordResetToken {

    private Long id;
    private Long userId;
    private byte[] tokenHash;
    private java.sql.Timestamp expiresAt;
    private java.sql.Timestamp usedAt;
    private java.sql.Timestamp createdAt;

    public PasswordResetToken() {
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

    public byte[] getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(byte[] tokenHash) {
        this.tokenHash = tokenHash;
    }

    public java.sql.Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(java.sql.Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public java.sql.Timestamp getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(java.sql.Timestamp usedAt) {
        this.usedAt = usedAt;
    }

    public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.sql.Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
