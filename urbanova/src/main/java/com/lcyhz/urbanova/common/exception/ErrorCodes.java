package com.lcyhz.urbanova.common.exception;

public final class ErrorCodes {
    private ErrorCodes() {
    }

    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";
    public static final String AUTH_TOKEN_EXPIRED = "AUTH_TOKEN_EXPIRED";
    public static final String AUTH_FORBIDDEN = "AUTH_FORBIDDEN";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String BOOKING_CONFLICT = "BOOKING_CONFLICT";
    public static final String SCOOTER_NOT_AVAILABLE = "SCOOTER_NOT_AVAILABLE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
}

