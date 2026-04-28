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
