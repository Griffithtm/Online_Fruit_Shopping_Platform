package model;

import java.io.Serializable;

public final class SessionUser implements Serializable {

    private final long id;
    private final String name, role;

    public SessionUser(long id, String name, String role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

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
