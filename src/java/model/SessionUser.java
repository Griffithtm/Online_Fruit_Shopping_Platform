package model;

import java.io.Serializable;

public final class SessionUser implements Serializable {

    private final long id;
    private final String name, role, credential;

    public SessionUser(long id, String name, String role) {
        this(id,name,role,null);
    }

    public SessionUser(long id,String name,String role,String credential) {
        this.credential=credential;
        this.id = id;
        this.name = name;
        this.role = role;
    }

    /** Same session identity with an updated display name (after a profile change). */
    public SessionUser withName(String newName) {
        return new SessionUser(id, newName, role, credential);
    }

    public boolean credentialsMatch(String hash) { return credential != null && credential.equals(hash); }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }
}
