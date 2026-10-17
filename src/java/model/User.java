package model;

/**
 * Database entity: User.
 */
public class User {

    private Long id;
    private String email;
    private String passwordHash;
    private String fullName;
    private String phone;
    private String role;
    private Long membershipTierId;
    private Integer pointsBalance;
    private String status;
    private java.sql.Timestamp lastLoginAt;
    private java.sql.Timestamp createdAt;
    private java.sql.Timestamp updatedAt;

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getMembershipTierId() {
        return membershipTierId;
    }

    public void setMembershipTierId(Long membershipTierId) {
        this.membershipTierId = membershipTierId;
    }

    public Integer getPointsBalance() {
        return pointsBalance;
    }

    public void setPointsBalance(Integer pointsBalance) {
        this.pointsBalance = pointsBalance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.sql.Timestamp getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(java.sql.Timestamp lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
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
