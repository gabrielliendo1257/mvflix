package com.guille.mvflix.devseed;

/**
 * Usuario de prueba de un entorno dev, definido de forma centralizada en
 * {@code dev-users.yaml} y consumido por todos los servicios del ecosistema.
 */
public class DevUser {

    private String username;

    private String email;

    private String password = "";

    private String role = "CUSTOMER";

    private String plan = "FREE";

    private long quotaBytes = 10L * 1024 * 1024 * 1024;

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPlan() {
        return this.plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public long getQuotaBytes() {
        return this.quotaBytes;
    }

    public void setQuotaBytes(long quotaBytes) {
        this.quotaBytes = quotaBytes;
    }
}
