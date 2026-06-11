package com.library.security;

/**
 * ThreadLocal holder for the selected login role.
 * Allows {@link MultiUserDetailsServiceImpl} to search the correct table first
 * when the same username exists in multiple user tables.
 */
public final class LoginRoleContext {

    private static final ThreadLocal<String> SELECTED_ROLE = new ThreadLocal<>();

    private LoginRoleContext() {}

    public static void set(String role) {
        SELECTED_ROLE.set(role);
    }

    public static String get() {
        return SELECTED_ROLE.get();
    }

    public static void clear() {
        SELECTED_ROLE.remove();
    }
}
