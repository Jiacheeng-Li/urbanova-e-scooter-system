package com.lcyhz.urbanova.security;

import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import org.springframework.http.HttpStatus;

public final class AuthContext {
    private static final ThreadLocal<AuthUser> CURRENT_USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setCurrentUser(AuthUser authUser) {
        CURRENT_USER.set(authUser);
    }

    public static AuthUser getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static AuthUser getRequiredUser() {
        AuthUser authUser = CURRENT_USER.get();
        if (authUser == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED.value(), ErrorCodes.AUTH_FORBIDDEN, "Authentication required");
        }
        return authUser;
    }

    public static String getRequiredUserId() {
        return getRequiredUser().getUserId();
    }

    public static void requireRole(String role) {
        AuthUser authUser = getRequiredUser();
        if (!role.equals(authUser.getRole())) {
            throw new BusinessException(HttpStatus.FORBIDDEN.value(), ErrorCodes.AUTH_FORBIDDEN, "No permission for this operation");
        }
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
