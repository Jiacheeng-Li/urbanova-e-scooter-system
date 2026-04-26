CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NULL,
    role VARCHAR(20) NOT NULL,
    discount_category VARCHAR(20) NOT NULL,
    account_status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_user_id (user_id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id VARCHAR(40) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    refresh_token VARCHAR(128) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_sessions_session_id (session_id),
    UNIQUE KEY uk_auth_sessions_refresh_token (refresh_token),
    KEY idx_auth_sessions_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reset_token_id VARCHAR(40) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    reset_token VARCHAR(128) NOT NULL,
    expires_at DATETIME NOT NULL,
    used TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_password_reset_tokens_reset_token_id (reset_token_id),
    UNIQUE KEY uk_password_reset_tokens_reset_token (reset_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hire_options (
    id BIGINT NOT NULL AUTO_INCREMENT,
    hire_option_id VARCHAR(40) NOT NULL,
    code VARCHAR(10) NOT NULL,
    duration_minutes INT NOT NULL,
    base_price DECIMAL(10,2) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_hire_options_hire_option_id (hire_option_id),
    UNIQUE KEY uk_hire_options_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS discount_rules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    discount_rule_id VARCHAR(40) NOT NULL,
    type VARCHAR(30) NOT NULL,
    threshold_hours_per_week DECIMAL(10,2) NULL,
    percentage DECIMAL(5,2) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_discount_rules_discount_rule_id (discount_rule_id),
    UNIQUE KEY uk_discount_rules_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scooter_types (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type_code VARCHAR(40) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scooter_types_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scooters (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scooter_id VARCHAR(32) NOT NULL,
    type_code VARCHAR(40) NOT NULL,
    color VARCHAR(40) NULL,
    status VARCHAR(20) NOT NULL,
    battery_percent INT NOT NULL DEFAULT 100,
    qr_code_id VARCHAR(32) NOT NULL,
    battery_updated_at DATETIME NULL,
    charge_started_at DATETIME NULL,
    low_battery_alerted_at DATETIME NULL,
    lat DECIMAL(10,6) NULL,
    lng DECIMAL(10,6) NULL,
    zone VARCHAR(80) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scooters_scooter_id (scooter_id),
    UNIQUE KEY uk_scooters_qr_code_id (qr_code_id),
    KEY idx_scooters_status (status),
    KEY idx_scooters_type_code (type_code),
    CONSTRAINT fk_scooters_type_code FOREIGN KEY (type_code) REFERENCES scooter_types (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_locations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(36) NOT NULL,
    lat DECIMAL(10,6) NOT NULL,
    lng DECIMAL(10,6) NOT NULL,
    source VARCHAR(30) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_locations_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_methods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_method_id VARCHAR(40) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    brand VARCHAR(20) NOT NULL,
    last4 VARCHAR(4) NOT NULL,
    expiry_month INT NOT NULL,
    expiry_year INT NOT NULL,
    label VARCHAR(60) NULL,
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_methods_payment_method_id (payment_method_id),
    KEY idx_payment_methods_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    booking_id VARCHAR(32) NOT NULL,
    booking_ref VARCHAR(32) NOT NULL,
    customer_type VARCHAR(20) NOT NULL,
    user_id VARCHAR(36) NULL,
    guest_name VARCHAR(100) NULL,
    guest_email VARCHAR(255) NULL,
    guest_phone VARCHAR(30) NULL,
    scooter_id VARCHAR(32) NOT NULL,
    hire_option_id VARCHAR(40) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    actual_start_at DATETIME NULL,
    actual_end_at DATETIME NULL,
    status VARCHAR(20) NOT NULL,
    price_base DECIMAL(10,2) NOT NULL,
    price_discount DECIMAL(10,2) NOT NULL,
    price_final DECIMAL(10,2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    created_by_role VARCHAR(20) NOT NULL,
    cancel_reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bookings_booking_id (booking_id),
    UNIQUE KEY uk_bookings_booking_ref (booking_ref),
    KEY idx_bookings_user_id (user_id),
    KEY idx_bookings_scooter_id (scooter_id),
    KEY idx_bookings_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_id VARCHAR(40) NOT NULL,
    booking_id VARCHAR(32) NOT NULL,
    user_id VARCHAR(36) NULL,
    amount DECIMAL(10,2) NOT NULL,
    method VARCHAR(20) NOT NULL,
    payment_method_id VARCHAR(40) NULL,
    status VARCHAR(20) NOT NULL,
    simulated_outcome VARCHAR(20) NULL,
    refunded_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payments_payment_id (payment_id),
    KEY idx_payments_booking_id (booking_id),
    KEY idx_payments_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS booking_confirmations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    confirmation_id VARCHAR(40) NOT NULL,
    booking_id VARCHAR(32) NOT NULL,
    user_id VARCHAR(36) NULL,
    recipient_email VARCHAR(255) NULL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    message VARCHAR(255) NOT NULL,
    resend_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_booking_confirmations_confirmation_id (confirmation_id),
    KEY idx_booking_confirmations_booking_id (booking_id),
    KEY idx_booking_confirmations_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    notification_id VARCHAR(40) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(120) NOT NULL,
    message VARCHAR(255) NOT NULL,
    read_flag TINYINT(1) NOT NULL DEFAULT 0,
    related_booking_id VARCHAR(32) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notifications_notification_id (notification_id),
    KEY idx_notifications_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS booking_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(40) NOT NULL,
    booking_id VARCHAR(32) NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    actor_user_id VARCHAR(36) NULL,
    actor_role VARCHAR(20) NOT NULL,
    details VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_booking_events_event_id (event_id),
    KEY idx_booking_events_booking_id (booking_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS issues (
    id BIGINT NOT NULL AUTO_INCREMENT,
    issue_id VARCHAR(40) NOT NULL,
    reporter_user_id VARCHAR(36) NOT NULL,
    booking_id VARCHAR(32) NULL,
    scooter_id VARCHAR(32) NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    manager_feedback VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_issues_issue_id (issue_id),
    KEY idx_issues_reporter_user_id (reporter_user_id),
    KEY idx_issues_priority (priority),
    KEY idx_issues_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS issue_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    comment_id VARCHAR(40) NOT NULL,
    issue_id VARCHAR(40) NOT NULL,
    author_user_id VARCHAR(36) NULL,
    author_role VARCHAR(20) NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_issue_comments_comment_id (comment_id),
    KEY idx_issue_comments_issue_id (issue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    audit_log_id VARCHAR(40) NOT NULL,
    actor_user_id VARCHAR(36) NULL,
    actor_role VARCHAR(20) NOT NULL,
    action VARCHAR(60) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id VARCHAR(40) NULL,
    details VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_audit_logs_audit_log_id (audit_log_id),
    KEY idx_audit_logs_actor_user_id (actor_user_id),
    KEY idx_audit_logs_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS idempotency_keys (
    id BIGINT NOT NULL AUTO_INCREMENT,
    idempotency_key VARCHAR(80) NOT NULL,
    scope VARCHAR(80) NOT NULL,
    request_hash VARCHAR(64) NULL,
    response_code INT NULL,
    response_ref VARCHAR(80) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_keys_scope_key (scope, idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
