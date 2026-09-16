#ifndef ANALYTICS_LOGGER_H
#define ANALYTICS_LOGGER_H

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    ANALYTICS_LOG_DEBUG,
    ANALYTICS_LOG_INFO,
    ANALYTICS_LOG_WARN,
    ANALYTICS_LOG_ERROR
} AnalyticsLogLevel;

/** Установить минимальный уровень логирования (по умолчанию INFO) */
void analytics_log_set_level(AnalyticsLogLevel level);

/** Логирование с форматированием (printf-style). message и аргументы не должны быть NULL. */
void analytics_log_debug(const char* message, ...);
void analytics_log_info(const char* message, ...);
void analytics_log_warn(const char* message, ...);
void analytics_log_error(const char* message, ...);

#ifdef __cplusplus
}
#endif

#endif /* ANALYTICS_LOGGER_H */
