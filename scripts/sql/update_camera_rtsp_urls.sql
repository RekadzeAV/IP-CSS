-- Script: Update Camera RTSP URLs with Credentials
-- Date: 2026-06-09
-- Purpose: Update RTSP URLs for 7 cameras in surveillance database

-- Update Camera 17
UPDATE camera SET 
    url = 'rtsp://survival:1234567890qazxs@192.168.10.17:554/stream1',
    username = '\',
    password = '\',
    updated_at = CURRENT_TIMESTAMP
WHERE id = '3f723935-6057-455f-99c3-4fcd8cb107a0';

-- Update Camera 20
UPDATE camera SET 
    url = 'rtsp://survival:1234567890qazxs@192.168.10.20:554/stream1',
    username = '\',
    password = '\',
    updated_at = CURRENT_TIMESTAMP
WHERE id = '62fb2974-c2d7-4880-af30-9165c96e8a8d';

-- Update Camera 21
UPDATE camera SET 
    url = 'rtsp://survival:1234567890qazxs@192.168.10.21:554/stream1',
    username = '\',
    password = '\',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'd854beef-38ae-4490-8b31-926bc8149a4b';

-- Update Camera 22
UPDATE camera SET 
    url = 'rtsp://survival:1234567890qazxs@192.168.10.22:554/stream1',
    username = '\',
    password = '\',
    updated_at = CURRENT_TIMESTAMP
WHERE id = '895061a2-14d2-4500-ac74-24ecde706499';

-- Update Camera 23
UPDATE camera SET 
    url = 'rtsp://survival:1234567890qazxs@192.168.10.23:554/stream1',
    username = '\',
    password = '\',
    updated_at = CURRENT_TIMESTAMP
WHERE id = '0d32d3c8-a97b-4df8-a8f4-6944177d43f1';

-- Update Camera 24
UPDATE camera SET 
    url = 'rtsp://survival:1234567890qazxs@192.168.10.24:554/stream1',
    username = '\',
    password = '\',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'ae30bc74-5231-456a-9bd1-5baa099358ff';

-- Update Camera 26
UPDATE camera SET 
    url = 'rtsp://survival:1234567890qazxs@192.168.10.26:554/stream1',
    username = '\',
    password = '\',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'd0da42c5-596b-4cea-9593-f28ef34537dd';

-- Verify updates
SELECT id, name, url, username, updated_at 
FROM camera 
WHERE id IN (
    '3f723935-6057-455f-99c3-4fcd8cb107a0',
    '62fb2974-c2d7-4880-af30-9165c96e8a8d',
    'd854beef-38ae-4490-8b31-926bc8149a4b',
    '895061a2-14d2-4500-ac74-24ecde706499',
    '0d32d3c8-a97b-4df8-a8f4-6944177d43f1',
    'ae30bc74-5231-456a-9bd1-5baa099358ff',
    'd0da42c5-596b-4cea-9593-f28ef34537dd'
)
ORDER BY name;
