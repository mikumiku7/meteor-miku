package com.github.mikumiku.addon.auth.security;

import java.util.Map;

/**
 * 验证结果类
 */
public class AuthResult {
    private final boolean success;
    private final String message;
    private final String data;

    AuthResult(boolean success, String message, String data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static AuthResult success(String message, String data) {
        return new AuthResult(true, message, data);
    }

    public static AuthResult failure(String message) {
        return new AuthResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "AuthResult{success=" + success + ", message='" + message + "', data=" + data + "}";
    }
}
