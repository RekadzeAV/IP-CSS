-- Flyway Migration: PostgreSQL Production Optimizations
-- Adds partitioning, materialized views, and vacuum configuration for production workloads

-- 1. Partitioning for recording table by month (improves query performance for large datasets)
-- Only apply if table is empty or new (migration-safe)
DO $$
DECLARE
    rec RECORD;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_class WHERE relname = 'recording_part'
          AND relnamespace = 'public'::regnamespace
    ) THEN
        -- Create partitioned table
        CREATE TABLE IF NOT EXISTS recording_part (
            id TEXT NOT NULL,
            camera_id TEXT NOT NULL,
            camera_name TEXT,
            start_time BIGINT NOT NULL,
            end_time BIGINT,
            duration INTEGER NOT NULL,
            file_path TEXT NOT NULL,
            file_size INTEGER NOT NULL DEFAULT 0,
            format TEXT NOT NULL DEFAULT 'MP4',
            quality TEXT NOT NULL DEFAULT 'HIGH',
            status TEXT NOT NULL DEFAULT 'COMPLETED',
            thumbnail_url TEXT,
            created_at BIGINT NOT NULL,
            partition_key BIGINT NOT NULL
        ) PARTITION BY RANGE (partition_key);

        -- Create partitions for next 12 months (по одной через EXECUTE;
        -- исправлено: переменная объявлена, цикл вместо единственного INTO STRICT)
        FOR rec IN
            SELECT
                month_start,
                format(
                    'CREATE TABLE recording_%s PARTITION OF recording_part FOR VALUES FROM (%L) TO (%L)',
                    to_char(month_start, 'YYYY_MM'),
                    EXTRACT(EPOCH FROM month_start)::bigint,
                    EXTRACT(EPOCH FROM month_start + INTERVAL '1 month')::bigint
                ) AS stmt
            FROM generate_series(
                date_trunc('month', NOW())::date,
                date_trunc('month', NOW() + INTERVAL '12 months')::date,
                '1 month'::interval
            ) AS month_start
        LOOP
            IF NOT EXISTS (
                SELECT 1 FROM pg_class
                WHERE relname = format('recording_%s', to_char(rec.month_start, 'YYYY_MM'))
                  AND relnamespace = 'public'::regnamespace
            ) THEN
                EXECUTE rec.stmt;
            END IF;
        END LOOP;
        
        -- Create index on partition key
        CREATE INDEX IF NOT EXISTS recording_part_partition_key_idx ON recording_part(partition_key);
        
        RAISE NOTICE 'Recording partitioning created successfully';
    END IF;
END $$;

-- 2. Materialized view for camera statistics (pre-computed aggregates)
CREATE MATERIALIZED VIEW IF NOT EXISTS camera_statistics_mv AS
SELECT 
    c.id as camera_id,
    c.name as camera_name,
    c.status,
    COUNT(r.id) as total_recordings,
    SUM(r.duration) as total_recording_duration,
    SUM(r.file_size) as total_storage_used,
    COUNT(e.id) as total_events,
    COUNT(CASE WHEN e.acknowledged = 0 THEN 1 END) as unacknowledged_events,
    MAX(r.start_time) as last_recording_time,
    MAX(e.timestamp) as last_event_time
FROM camera c
LEFT JOIN recording r ON c.id = r.camera_id
LEFT JOIN event e ON c.id = e.camera_id
GROUP BY c.id, c.name, c.status;

-- Create indexes on materialized view
CREATE UNIQUE INDEX IF NOT EXISTS camera_statistics_mv_camera_id_idx ON camera_statistics_mv(camera_id);
CREATE INDEX IF NOT EXISTS camera_statistics_mv_status_idx ON camera_statistics_mv(status);

-- 3. Materialized view for recent events (last 24 hours)
-- Note: e.* заменён явным списком — в таблице event уже есть camera_name,
-- дубликат с c.name as camera_name ломал миграцию (column specified more than once)
CREATE MATERIALIZED VIEW IF NOT EXISTS recent_events_mv AS
SELECT 
    e.id,
    e.camera_id,
    e.type,
    e.severity,
    e.timestamp,
    e.description,
    e.metadata,
    e.acknowledged,
    e.acknowledged_at,
    e.acknowledged_by,
    e.thumbnail_url,
    e.video_url,
    c.name as camera_name,
    c.status as camera_status
FROM event e
JOIN camera c ON e.camera_id = c.id
WHERE e.timestamp > (EXTRACT(EPOCH FROM NOW() - INTERVAL '24 hours')::bigint)
ORDER BY e.timestamp DESC;

CREATE INDEX IF NOT EXISTS recent_events_mv_timestamp_idx ON recent_events_mv(timestamp DESC);
CREATE INDEX IF NOT EXISTS recent_events_mv_camera_id_idx ON recent_events_mv(camera_id);

-- 4. Automatic vacuum and analyze configuration for high-write tables
ALTER TABLE camera SET (autovacuum_enabled = true, autovacuum_vacuum_scale_factor = 0.1);
ALTER TABLE recording SET (autovacuum_vacuum_scale_factor = 0.05, autovacuum_analyze_scale_factor = 0.02);
ALTER TABLE event SET (autovacuum_vacuum_scale_factor = 0.05, autovacuum_analyze_scale_factor = 0.02);
ALTER TABLE notification SET (autovacuum_vacuum_scale_factor = 0.1, autovacuum_analyze_scale_factor = 0.05);

-- 5. Connection pool monitoring table (for HikariCP metrics)
CREATE TABLE IF NOT EXISTS connection_pool_metrics (
    id SERIAL PRIMARY KEY,
    pool_name TEXT NOT NULL DEFAULT 'primary',
    active_connections INTEGER NOT NULL DEFAULT 0,
    idle_connections INTEGER NOT NULL DEFAULT 0,
    total_connections INTEGER NOT NULL DEFAULT 0,
    threads_waiting INTEGER NOT NULL DEFAULT 0,
    recorded_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS connection_pool_metrics_recorded_at_idx ON connection_pool_metrics(recorded_at DESC);

-- 6. Query performance tracking (slow query log)
CREATE TABLE IF NOT EXISTS query_performance_log (
    id SERIAL PRIMARY KEY,
    query_text TEXT NOT NULL,
    execution_time_ms INTEGER NOT NULL,
    rows_affected INTEGER NOT NULL DEFAULT 0,
    recorded_at BIGINT NOT NULL,
    is_slow_query INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS query_performance_log_recorded_at_idx ON query_performance_log(recorded_at DESC);
CREATE INDEX IF NOT EXISTS query_performance_log_slow_idx ON query_performance_log(is_slow_query);

-- 7. Database health check table
CREATE TABLE IF NOT EXISTS db_health_check (
    id SERIAL PRIMARY KEY,
    check_type TEXT NOT NULL,
    status TEXT NOT NULL,
    message TEXT,
    details TEXT,
    checked_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS db_health_check_checked_at_idx ON db_health_check(checked_at DESC);
CREATE INDEX IF NOT EXISTS db_health_check_status_idx ON db_health_check(status);

-- 8. Vacuum configuration for maintenance
-- Note: These are suggestions for pg_auto_vacuum configuration
-- In production, adjust based on actual workload
DO $$
BEGIN
    -- Check if we can set maintenance_work_mem (requires superuser)
    BEGIN
        EXECUTE 'ALTER SYSTEM SET maintenance_work_mem = ''512MB''';
        RAISE NOTICE 'maintenance_work_mem set to 512MB';
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'Could not set maintenance_work_mem (requires superuser): %', SQLERRM;
    END;
END $$;

-- 9. Create function for refreshing materialized views concurrently
CREATE OR REPLACE FUNCTION refresh_materialized_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY camera_statistics_mv;
    REFRESH MATERIALIZED VIEW CONCURRENTLY recent_events_mv;
END;
$$ LANGUAGE plpgsql;

-- 10. Create function for logging slow queries
CREATE OR REPLACE FUNCTION log_slow_query(
    p_query_text TEXT,
    p_execution_time_ms INTEGER,
    p_rows_affected INTEGER DEFAULT 0,
    p_is_slow INTEGER DEFAULT 0
)
RETURNS void AS $$
BEGIN
    INSERT INTO query_performance_log (query_text, execution_time_ms, rows_affected, recorded_at, is_slow_query)
    VALUES (p_query_text, p_execution_time_ms, p_rows_affected, EXTRACT(EPOCH FROM NOW())::bigint, p_is_slow);
END;
$$ LANGUAGE plpgsql;

-- 11. Create function for health check
CREATE OR REPLACE FUNCTION db_health_check()
RETURNS TABLE(
    check_type TEXT,
    status TEXT,
    message TEXT,
    checked_at BIGINT
) AS $$
DECLARE
    v_checked_at BIGINT := EXTRACT(EPOCH FROM NOW())::bigint;
    v_db_size BIGINT;
    v_table_count INTEGER;
    v_last_vacuum BIGINT;
BEGIN
    -- Get database size
    SELECT pg_database_size(current_database()) INTO v_db_size;
    
    -- Get table count
    SELECT COUNT(*) INTO v_table_count 
    FROM information_schema.tables 
    WHERE table_schema = 'public';
    
    -- Get last vacuum time for recording table
    SELECT MAX(last_autovacuum) INTO v_last_vacuum 
    FROM pg_stat_user_tables 
    WHERE relname = 'recording';
    
    -- Insert health check results
    INSERT INTO db_health_check (check_type, status, message, checked_at)
    VALUES 
        ('database_size', 'OK', format('Database size: %s', pg_size_pretty(v_db_size)), v_checked_at),
        ('table_count', 'OK', format('Total tables: %s', v_table_count), v_checked_at),
        ('last_vacuum', CASE WHEN v_last_vacuum IS NOT NULL THEN 'OK' ELSE 'WARNING' END, 
         CASE WHEN v_last_vacuum IS NOT NULL THEN 'Last vacuum: ' || to_timestamp(v_last_vacuum) 
              ELSE 'No vacuum recorded' END, v_checked_at);
    
    -- Return results
    RETURN QUERY 
    SELECT check_type, status, message, checked_at 
    FROM db_health_check 
    WHERE checked_at = v_checked_at
    ORDER BY check_type;
END;
$$ LANGUAGE plpgsql;
