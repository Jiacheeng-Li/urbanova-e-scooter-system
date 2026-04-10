package com.lcyhz.urbanova.domain;

public final class DomainConstants {
    private DomainConstants() {
    }

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ACCOUNT_ACTIVE = "ACTIVE";
    public static final String DISCOUNT_NONE = "NONE";
    public static final String CUSTOMER_TYPE_REGISTERED = "REGISTERED";
    public static final String PAYMENT_STATUS_UNPAID = "UNPAID";
    public static final String CURRENCY_GBP = "GBP";

    public static final class ScooterStatus {
        private ScooterStatus() {
        }

        public static final String AVAILABLE = "AVAILABLE";
        public static final String RESERVED = "RESERVED";
        public static final String IN_USE = "IN_USE";
        public static final String MAINTENANCE = "MAINTENANCE";
        public static final String UNAVAILABLE = "UNAVAILABLE";
    }

    public static final class BookingStatus {
        private BookingStatus() {
        }

        public static final String CONFIRMED = "CONFIRMED";
        public static final String CANCELLED = "CANCELLED";
    }
}
