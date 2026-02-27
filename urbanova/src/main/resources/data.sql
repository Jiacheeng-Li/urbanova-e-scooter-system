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

INSERT INTO scooters (scooter_id, status, battery_percent, lat, lng, zone)
VALUES
    ('SCO-0001', 'AVAILABLE', 92, 51.507400, -0.127800, 'ZONE-A'),
    ('SCO-0002', 'AVAILABLE', 88, 51.506800, -0.128600, 'ZONE-A'),
    ('SCO-0003', 'AVAILABLE', 77, 51.508200, -0.126900, 'ZONE-B'),
    ('SCO-0004', 'AVAILABLE', 81, 51.509000, -0.125400, 'ZONE-B'),
    ('SCO-0005', 'AVAILABLE', 95, 51.505900, -0.129700, 'ZONE-C')
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    battery_percent = VALUES(battery_percent),
    lat = VALUES(lat),
    lng = VALUES(lng),
    zone = VALUES(zone);

