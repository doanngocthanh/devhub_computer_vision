package com.devhub.ocr.app.systems.auth;

/**
 * Thread-local holder for the current authenticated user for the duration of a request.
 */
public final class AuthContext {

    private static final ThreadLocal<UserObject> current = new ThreadLocal<>();

    private AuthContext() {}

    public static void set(UserObject u) {
        current.set(u);
    }

    public static UserObject get() {
        return current.get();
    }

    public static void clear() {
        current.remove();
    }
}
