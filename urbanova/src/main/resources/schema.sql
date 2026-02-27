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

CREATE TABLE IF NOT EXISTS scooters (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scooter_id VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL,
    battery_percent INT NOT NULL DEFAULT 100,
    lat DECIMAL(10,6) NULL,
    lng DECIMAL(10,6) NULL,
    zone VARCHAR(80) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scooters_scooter_id (scooter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    booking_id VARCHAR(32) NOT NULL,
    booking_ref VARCHAR(32) NOT NULL,
    customer_type VARCHAR(20) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    scooter_id VARCHAR(32) NOT NULL,
    hire_option_id VARCHAR(40) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
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

