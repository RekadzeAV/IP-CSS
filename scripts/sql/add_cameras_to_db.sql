-- Script: Add 7 Cameras to Database
-- Date: 2026-06-09
-- Purpose: Add cameras from working surveillance network

-- Insert Camera 17
INSERT INTO camera (id, name, ip_address, port, url, username, password, location, status, created_at, updated_at)
VALUES (
    '3f723935-6057-455f-99c3-4fcd8cb107a0',
    'Camera 17',
    '192.168.10.17',
    554,
    'rtsp://survival:1234567890qazxs@192.168.10.17:554/stream1',
    'survival',
    '1234567890qazxs',
    'Building 1, Floor 1',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    ip_address = EXCLUDED.ip_address,
    url = EXCLUDED.url,
    username = EXCLUDED.username,
    password = EXCLUDED.password,
    updated_at = CURRENT_TIMESTAMP;

-- Insert Camera 20
INSERT INTO camera (id, name, ip_address, port, url, username, password, location, status, created_at, updated_at)
VALUES (
    '62fb2974-c2d7-4880-af30-9165c96e8a8d',
    'Camera 20',
    '192.168.10.20',
    554,
    'rtsp://survival:1234567890qazxs@192.168.10.20:554/stream1',
    'survival',
    '1234567890qazxs',
    'Building 1, Floor 2',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    ip_address = EXCLUDED.ip_address,
    url = EXCLUDED.url,
    username = EXCLUDED.username,
    password = EXCLUDED.password,
    updated_at = CURRENT_TIMESTAMP;

-- Insert Camera 21
INSERT INTO camera (id, name, ip_address, port, url, username, password, location, status, created_at, updated_at)
VALUES (
    'd854beef-38ae-4490-8b31-926bc8149a4b',
    'Camera 21',
    '192.168.10.21',
    554,
    'rtsp://survival:1234567890qazxs@192.168.10.21:554/stream1',
    'survival',
    '1234567890qazxs',
    'Building 2, Floor 1',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    ip_address = EXCLUDED.ip_address,
    url = EXCLUDED.url,
    username = EXCLUDED.username,
    password = EXCLUDED.password,
    updated_at = CURRENT_TIMESTAMP;

-- Insert Camera 22
INSERT INTO camera (id, name, ip_address, port, url, username, password, location, status, created_at, updated_at)
VALUES (
    '895061a2-14d2-4500-ac74-24ecde706499',
    'Camera 22',
    '192.168.10.22',
    554,
    'rtsp://survival:1234567890qazxs@192.168.10.22:554/stream1',
    'survival',
    '1234567890qazxs',
    'Building 2, Floor 2',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    ip_address = EXCLUDED.ip_address,
    url = EXCLUDED.url,
    username = EXCLUDED.username,
    password = EXCLUDED.password,
    updated_at = CURRENT_TIMESTAMP;

-- Insert Camera 23
INSERT INTO camera (id, name, ip_address, port, url, username, password, location, status, created_at, updated_at)
VALUES (
    '0d32d3c8-a97b-4df8-a8f4-6944177d43f1',
    'Camera 23',
    '192.168.10.23',
    554,
    'rtsp://survival:1234567890qazxs@192.168.10.23:554/stream1',
    'survival',
    '1234567890qazxs',
    'Building 3, Floor 1',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    ip_address = EXCLUDED.ip_address,
    url = EXCLUDED.url,
    username = EXCLUDED.username,
    password = EXCLUDED.password,
    updated_at = CURRENT_TIMESTAMP;

-- Insert Camera 24
INSERT INTO camera (id, name, ip_address, port, url, username, password, location, status, created_at, updated_at)
VALUES (
    'ae30bc74-5231-456a-9bd1-5baa099358ff',
    'Camera 24',
    '192.168.10.24',
    554,
    'rtsp://survival:1234567890qazxs@192.168.10.24:554/stream1',
    'survival',
    '1234567890qazxs',
    'Building 3, Floor 2',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    ip_address = EXCLUDED.ip_address,
    url = EXCLUDED.url,
    username = EXCLUDED.username,
    password = EXCLUDED.password,
    updated_at = CURRENT_TIMESTAMP;

-- Insert Camera 26
INSERT INTO camera (id, name, ip_address, port, url, username, password, location, status, created_at, updated_at)
VALUES (
    'd0da42c5-596b-4cea-9593-f28ef34537dd',
    'Camera 26',
    '192.168.10.26',
    554,
    'rtsp://survival:1234567890qazxs@192.168.10.26:554/stream1',
    'survival',
    '1234567890qazxs',
    'Building 4, Floor 1',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    ip_address = EXCLUDED.ip_address,
    url = EXCLUDED.url,
    username = EXCLUDED.username,
    password = EXCLUDED.password,
    updated_at = CURRENT_TIMESTAMP;

-- Verify insertions
SELECT id, name, ip_address, url, status FROM camera ORDER BY name;
