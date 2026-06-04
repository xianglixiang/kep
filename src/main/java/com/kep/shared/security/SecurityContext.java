package com.kep.shared.security;

public final class SecurityContext {

    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    private SecurityContext() {}

    public static void setCurrentUserId(Long userId) {
        CURRENT_USER.set(userId);
    }

    public static Long getCurrentUserId() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
