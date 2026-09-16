# ONVIF Unit Gap Matrix (1.4.1 / lifecycle)

## Mapping edge-cases

- `Конфликтующие признаки payload (priority)` -> `server/api/src/test/kotlin/com/company/ipcamera/server/service/OnvifEventMapperTest.kt` -> `mapToDomainEvent - topic priority wins over conflicting payload hints`
- `Отсутствующие поля payload` -> `server/api/src/test/kotlin/com/company/ipcamera/server/service/OnvifEventMapperTest.kt` -> `mapToDomainEvent - missing payload fields handled safely`
- `Невалидные источники Severity` -> `server/api/src/test/kotlin/com/company/ipcamera/server/service/OnvifEventMapperTest.kt` -> `mapToDomainEvent - invalid severity source in payload does not override mapping`

## Subscription lifecycle / retry

- `Retry после transient failure (pull loop)` -> `server/api/src/test/kotlin/com/company/ipcamera/server/service/OnvifEventSubscriptionServiceTest.kt` -> `pull point retry after transient failure eventually processes event`
- `Race add/remove камеры` -> `server/api/src/test/kotlin/com/company/ipcamera/server/service/OnvifEventSubscriptionServiceTest.kt` -> `concurrent subscribe unsubscribe same camera does not crash`
- `Идемпотентность ensureMonitoring` -> `server/api/src/test/kotlin/com/company/ipcamera/server/service/CameraEventMonitoringServiceJvmTest.kt` -> `ensureMonitoring is idempotent`
- `Retry ensureMonitoring после transient failure` -> `server/api/src/test/kotlin/com/company/ipcamera/server/service/CameraEventMonitoringServiceJvmTest.kt` -> `ensureMonitoring retries after transient start failure`
- `Race add/remove + ensureMonitoring` -> `server/api/src/test/kotlin/com/company/ipcamera/server/service/CameraEventMonitoringServiceJvmTest.kt` -> `race add remove camera during ensureMonitoring does not crash`

## UI realtime regression

- `Индикация потери/восстановления канала` -> `server/web/src/hooks/useWebSocket.test.tsx` -> `updates store when websocket disconnect and reconnect callbacks fire`
- `Устойчивость к пустому/частично невалидному payload` -> `server/web/src/hooks/useWebSocket.test.tsx` -> `ignores empty and partially invalid events payloads`
