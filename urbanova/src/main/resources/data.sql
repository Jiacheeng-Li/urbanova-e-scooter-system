INSERT INTO hire_options (hire_option_id, code, duration_minutes, base_price, active)
VALUES
    ('HIRE-H1', 'H1', 60, 3.00, 1),
    ('HIRE-H4', 'H4', 240, 12.00, 1),
    ('HIRE-D1', 'D1', 1440, 20.00, 1),
    ('HIRE-W1', 'W1', 10080, 60.00, 1)
ON DUPLICATE KEY UPDATE
    duration_minutes = VALUES(duration_minutes),
    base_price = VALUES(base_price),
    active = VALUES(active);

INSERT INTO discount_rules (discount_rule_id, type, threshold_hours_per_week, percentage, active)
VALUES
    ('DISC-FREQUENT', 'FREQUENT_USER', 8.00, 15.00, 1),
    ('DISC-STUDENT', 'STUDENT', NULL, 10.00, 1),
    ('DISC-SENIOR', 'SENIOR', NULL, 12.00, 1)
ON DUPLICATE KEY UPDATE
    threshold_hours_per_week = VALUES(threshold_hours_per_week),
    percentage = VALUES(percentage),
    active = VALUES(active);

INSERT INTO promotion_policies (
    promotion_policy_id, policy_code, name, category, description, percentage,
    min_age, max_age, min_completed_bookings, max_completed_bookings,
    holiday_campaign, stackable, priority, start_at, end_at, active
)
VALUES
    ('PRM-FIRST3', 'FIRST_THREE_RIDES', 'First three rides welcome offer', 'NEW_RIDER',
     'Applies to riders before they complete their third successful ride.', 8.00,
     NULL, NULL, 0, 2, 0, 1, 10,
     DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 6 DAY), 1),
    ('PRM-U18', 'UNDER_18_RIDER', 'Young rider support discount', 'AGE_BASED',
     'Supports riders aged 12 to 17 with a short-term age-based discount.', 6.00,
     12, 17, NULL, NULL, 0, 1, 20,
     DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 6 DAY), 1),
    ('PRM-45PLUS', 'AGE_45_PLUS', '45 plus rider support discount', 'AGE_BASED',
     'Supports riders aged 45 and above with a short-term age-based discount.', 7.00,
     45, NULL, NULL, NULL, 0, 1, 25,
     DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 6 DAY), 1),
    ('PRM-LOYALTY5', 'LOYALTY_5_PLUS', 'Loyalty tier 1', 'LOYALTY',
     'Applies after five completed rides.', 5.00,
     NULL, NULL, 5, 9, 0, 1, 30,
     DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 6 DAY), 1),
    ('PRM-LOYALTY10', 'LOYALTY_10_PLUS', 'Loyalty tier 2', 'LOYALTY',
     'Applies after ten completed rides.', 8.00,
     NULL, NULL, 10, 19, 0, 1, 31,
     DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 6 DAY), 1),
    ('PRM-LOYALTY20', 'LOYALTY_20_PLUS', 'Loyalty tier 3', 'LOYALTY',
     'Applies after twenty completed rides.', 12.00,
     NULL, NULL, 20, NULL, 0, 1, 32,
     DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY), DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 6 DAY), 1),
    ('PRM-LABOUR2026', 'LABOUR_DAY_2026', 'Labour Day holiday campaign', 'HOLIDAY',
     'Seasonal promotion for the Labour Day holiday period.', 10.00,
     NULL, NULL, NULL, NULL, 1, 1, 40,
     '2026-05-01 00:00:00', '2026-05-07 23:59:59', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    category = VALUES(category),
    description = VALUES(description),
    percentage = VALUES(percentage),
    min_age = VALUES(min_age),
    max_age = VALUES(max_age),
    min_completed_bookings = VALUES(min_completed_bookings),
    max_completed_bookings = VALUES(max_completed_bookings),
    holiday_campaign = VALUES(holiday_campaign),
    stackable = VALUES(stackable),
    priority = VALUES(priority),
    start_at = VALUES(start_at),
    end_at = VALUES(end_at),
    active = VALUES(active);

INSERT INTO scooter_types (type_code, display_name, image_url, description, active)
VALUES
    ('ANDROMEDA', 'ANDROMEDA', '/images/scooter-types/andromeda.png', 'High-performance urban scooter.', 1),
    ('GALAXY_SEAT', 'GALAXY Seat', '/images/scooter-types/galaxy-seat.png', 'Comfort-focused seated scooter.', 1),
    ('LUNAR_LITE', 'LUNAR Lite', '/images/scooter-types/lunar-lite.png', 'Lightweight commuter scooter.', 1),
    ('NEBULA_FAMILY', 'NEBULA Family', '/images/scooter-types/nebula-family.png', 'Family-friendly multi-rider design.', 1),
    ('ORION_ULTRA', 'ORION Ultra', '/images/scooter-types/orion-ultra.png', 'Premium long-range scooter.', 1)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    image_url = VALUES(image_url),
    description = VALUES(description),
    active = VALUES(active);

INSERT INTO scooters (scooter_id, type_code, color, status, battery_percent, qr_code_id, battery_updated_at, lat, lng, zone)
VALUES
    ('SCO-0001', 'ANDROMEDA', NULL, 'AVAILABLE', 92, 'QR-SCO0001', CURRENT_TIMESTAMP, 51.507400, -0.127800, 'ZONE-A'),
    ('SCO-0002', 'GALAXY_SEAT', NULL, 'AVAILABLE', 88, 'QR-SCO0002', CURRENT_TIMESTAMP, 51.506800, -0.128600, 'ZONE-A'),
    ('SCO-0003', 'LUNAR_LITE', NULL, 'AVAILABLE', 77, 'QR-SCO0003', CURRENT_TIMESTAMP, 51.508200, -0.126900, 'ZONE-B'),
    ('SCO-0004', 'NEBULA_FAMILY', NULL, 'AVAILABLE', 81, 'QR-SCO0004', CURRENT_TIMESTAMP, 51.509000, -0.125400, 'ZONE-B'),
    ('SCO-0005', 'ORION_ULTRA', NULL, 'AVAILABLE', 95, 'QR-SCO0005', CURRENT_TIMESTAMP, 51.505900, -0.129700, 'ZONE-C')
ON DUPLICATE KEY UPDATE
    type_code = VALUES(type_code),
    color = VALUES(color),
    status = VALUES(status),
    battery_percent = VALUES(battery_percent),
    qr_code_id = VALUES(qr_code_id),
    battery_updated_at = VALUES(battery_updated_at),
    lat = VALUES(lat),
    lng = VALUES(lng),
    zone = VALUES(zone);
