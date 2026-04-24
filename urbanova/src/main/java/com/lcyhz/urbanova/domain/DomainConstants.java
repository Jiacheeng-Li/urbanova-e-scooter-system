package com.lcyhz.urbanova.domain;

public final class DomainConstants {
    private DomainConstants() {
    }

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_STAFF = "STAFF";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_SYSTEM = "SYSTEM";
    public static final String ACCOUNT_ACTIVE = "ACTIVE";
    public static final String ACCOUNT_SUSPENDED = "SUSPENDED";
    public static final String ACCOUNT_DELETED = "DELETED";
    public static final String DISCOUNT_NONE = "NONE";
    public static final String DISCOUNT_STUDENT = "STUDENT";
    public static final String DISCOUNT_SENIOR = "SENIOR";
    public static final String CUSTOMER_TYPE_REGISTERED = "REGISTERED";
    public static final String CUSTOMER_TYPE_GUEST = "GUEST";
    public static final String PAYMENT_STATUS_UNPAID = "UNPAID";
    public static final String PAYMENT_STATUS_PAID = "PAID";
    public static final String PAYMENT_STATUS_PARTIAL = "PARTIAL";
    public static final String PAYMENT_STATUS_REFUNDED = "REFUNDED";
    public static final String CURRENCY_GBP = "GBP";

    public static final class PaymentMethodStatus {
        private PaymentMethodStatus() {
        }

        public static final String ACTIVE = "ACTIVE";
        public static final String EXPIRED = "EXPIRED";
        public static final String REMOVED = "REMOVED";
    }

    public static final class PaymentMethodType {
        private PaymentMethodType() {
        }

        public static final String SAVED_CARD = "SAVED_CARD";
        public static final String ONE_TIME_CARD = "ONE_TIME_CARD";
    }

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

        public static final String PENDING_PAYMENT = "PENDING_PAYMENT";
        public static final String CONFIRMED = "CONFIRMED";
        public static final String ACTIVE = "ACTIVE";
        public static final String COMPLETED = "COMPLETED";
        public static final String CANCELLED = "CANCELLED";
        public static final String EXPIRED = "EXPIRED";
    }

    public static final class PaymentStatus {
        private PaymentStatus() {
        }

        public static final String INITIATED = "INITIATED";
        public static final String SUCCEEDED = "SUCCEEDED";
        public static final String FAILED = "FAILED";
        public static final String REFUNDED = "REFUNDED";
    }

    public static final class PaymentOutcome {
        private PaymentOutcome() {
        }

        public static final String SUCCESS = "SUCCESS";
        public static final String FAILURE = "FAILURE";
    }

    public static final class NotificationType {
        private NotificationType() {
        }

        public static final String BOOKING_CONFIRMATION = "BOOKING_CONFIRMATION";
        public static final String BOOKING_CANCELLED = "BOOKING_CANCELLED";
        public static final String PAYMENT_UPDATED = "PAYMENT_UPDATED";
        public static final String ISSUE_UPDATED = "ISSUE_UPDATED";
    }

    public static final class ConfirmationStatus {
        private ConfirmationStatus() {
        }

        public static final String SENT = "SENT";
        public static final String RESENT = "RESENT";
    }

    public static final class IssuePriority {
        private IssuePriority() {
        }

        public static final String LOW = "LOW";
        public static final String HIGH = "HIGH";
        public static final String CRITICAL = "CRITICAL";
    }

    public static final class IssueStatus {
        private IssueStatus() {
        }

        public static final String OPEN = "OPEN";
        public static final String IN_REVIEW = "IN_REVIEW";
        public static final String RESOLVED = "RESOLVED";
        public static final String CLOSED = "CLOSED";
    }

    public static final class DiscountRuleType {
        private DiscountRuleType() {
        }

        public static final String FREQUENT_USER = "FREQUENT_USER";
        public static final String STUDENT = "STUDENT";
        public static final String SENIOR = "SENIOR";
    }
}
