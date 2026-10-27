package com.examlens.security;

import java.io.Serializable;

/** Authenticated principal stored in the HTTP session. */
public class SessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SESSION_KEY = "examLensUser";

    private final long id;
    private final String email;
    private final String displayName;
    private final String timezone;
    private final String theme;

    public SessionUser(long id, String email, String displayName, String timezone, String theme) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.timezone = timezone;
        this.theme = theme;
    }

    public long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getTheme() {
        return theme;
    }

    public String getInitials() {
        if (displayName == null || displayName.isBlank()) {
            return "?";
        }
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
