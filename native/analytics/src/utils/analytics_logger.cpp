#include "utils/analytics_logger.h"
#include <cstdarg>
#include <cstdio>
#include <mutex>

static AnalyticsLogLevel s_log_level = ANALYTICS_LOG_INFO;
static std::mutex s_log_mutex;

static const char* level_string(AnalyticsLogLevel level) {
    switch (level) {
        case ANALYTICS_LOG_DEBUG: return "DEBUG";
        case ANALYTICS_LOG_INFO:  return "INFO";
        case ANALYTICS_LOG_WARN:  return "WARN";
        case ANALYTICS_LOG_ERROR: return "ERROR";
        default: return "?";
    }
}

static void log_write(AnalyticsLogLevel level, const char* message, va_list args) {
    if (level < s_log_level) return;
    std::lock_guard<std::mutex> lock(s_log_mutex);
    fprintf(stderr, "[analytics][%s] ", level_string(level));
    vfprintf(stderr, message, args);
    fprintf(stderr, "\n");
}

void analytics_log_set_level(AnalyticsLogLevel level) {
    s_log_level = level;
}

void analytics_log_debug(const char* message, ...) {
    va_list args;
    va_start(args, message);
    log_write(ANALYTICS_LOG_DEBUG, message, args);
    va_end(args);
}

void analytics_log_info(const char* message, ...) {
    va_list args;
    va_start(args, message);
    log_write(ANALYTICS_LOG_INFO, message, args);
    va_end(args);
}

void analytics_log_warn(const char* message, ...) {
    va_list args;
    va_start(args, message);
    log_write(ANALYTICS_LOG_WARN, message, args);
    va_end(args);
}

void analytics_log_error(const char* message, ...) {
    va_list args;
    va_start(args, message);
    log_write(ANALYTICS_LOG_ERROR, message, args);
    va_end(args);
}
