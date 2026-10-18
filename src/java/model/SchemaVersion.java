package model;

/**
 * Database entity: SchemaVersion.
 */
public class SchemaVersion {

    private Integer version;
    private String description;
    private java.sql.Timestamp appliedAt;

    public SchemaVersion() {
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public java.sql.Timestamp getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(java.sql.Timestamp appliedAt) {
        this.appliedAt = appliedAt;
    }
}
