# 4.2 Кластеризация, балансировка нагрузки и репликация БД

## Реализовано

### 4.2.1 Кластеризация

- **4.2.1.1 Поддержка кластеров**
  - Режим кластера включается переменной `CLUSTER_ENABLED=true`.
  - Каждый узел имеет идентификатор: `NODE_ID` или `HOSTNAME`, иначе генерируется `node-<uuid>`.
  - Опционально задаётся `NODE_URL` (публичный URL узла для discovery).

- **4.2.1.2 Координация узлов**
  - **ClusterService**: регистрация узла в Redis (ключи `cluster:nodes:<nodeId>`, TTL по `CLUSTER_HEARTBEAT_SEC`).
  - Периодический heartbeat (по умолчанию каждые 30 сек).
  - Список узлов: `GET /api/v1/cluster/nodes` (только admin, JWT).
  - Идентификация текущего узла: `GET /api/v1/cluster/me` (публичный, для load balancer).

### 4.2.2 Балансировка нагрузки

- **Health с идентификатором узла**
  - В ответах `GET /api/v1/health` и `GET /api/v1/health/ready` при `CLUSTER_ENABLED=true` возвращаются поля `nodeId` и `clusterEnabled`.
  - Load balancer может использовать `/health/ready` для проверки готовности и при необходимости sticky session по `nodeId` (заголовок или тело ответа).

- **Рекомендуемая схема развёртывания**
  - Перед приложением ставится внешний балансировщик (nginx, HAProxy, cloud LB).
  - Алгоритмы: round-robin, least connections — настраиваются в балансировщике.
  - Примеры конфигурации см. ниже.

### 4.2.3 Репликация данных

- **4.2.3.1 Репликация БД**
  - Приложение подключается к primary PostgreSQL через `DATABASE_URL` (как и раньше).
  - Опционально задаётся **read replica**: `DATABASE_READ_REPLICA_URL` (JDBC). Создаётся отдельный HikariCP-пул только для чтения (`readOnly=true`).
  - Размер пула реплики: `DATABASE_READ_REPLICA_POOL_SIZE` (по умолчанию 5).

- **4.2.3.2 Синхронизация реплик**
  - Синхронизация и консистентность обеспечиваются PostgreSQL (streaming replication, logical replication).
  - Приложение не управляет репликацией; при наличии `DATABASE_READ_REPLICA_URL` может использовать реплику для read-only запросов (при дальнейшей доработке репозиториев).

## Переменные окружения

| Переменная | Описание |
|------------|----------|
| `CLUSTER_ENABLED` | `true` — включить режим кластера |
| `NODE_ID` | Идентификатор узла (опционально) |
| `NODE_URL` | Публичный URL узла (опционально) |
| `CLUSTER_HEARTBEAT_SEC` | Интервал heartbeat, сек (5–120, по умолчанию 30) |
| `DATABASE_READ_REPLICA_URL` | JDBC URL read replica (опционально) |
| `DATABASE_READ_REPLICA_POOL_SIZE` | Размер пула к реплике (по умолчанию 5) |

## API

| Метод | Путь | Описание | Доступ |
|-------|------|----------|--------|
| GET | `/api/v1/cluster/me` | Идентификатор текущего узла и флаг кластера | Публичный |
| GET | `/api/v1/cluster/nodes` | Список узлов кластера | Admin (JWT) |
| GET | `/api/v1/health` | Health с полями `nodeId`, `clusterEnabled` при кластере | Публичный |
| GET | `/api/v1/health/ready` | Readiness с `nodeId`, `clusterEnabled` при кластере | Публичный |

## Пример конфигурации nginx (round-robin)

```nginx
upstream ipcss_backend {
    # round-robin по умолчанию
    server 127.0.0.1:8080;
    server 127.0.0.1:8081;
    server 127.0.0.1:8082;
}

# least_conn (наименьшее число соединений):
# least_conn;
# server 127.0.0.1:8080;
# server 127.0.0.1:8081;

server {
    listen 80;
    server_name _;
    location / {
        proxy_pass http://ipcss_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Пример конфигурации HAProxy (least connections)

```haproxy
frontend http-in
    bind *:80
    default_backend ipcss_servers

backend ipcss_servers
    balance leastconn
    option httpchk GET /api/v1/health/ready
    http-check expect status 200
    server node1 127.0.0.1:8080 check
    server node2 127.0.0.1:8081 check
    server node3 127.0.0.1:8082 check
```

## Репликация PostgreSQL

- Primary: настройка `wal_level=replica`, создание пользователя репликации и слотов репликации.
- Реплика: `primary_conninfo` и восстановление из бэка primary или pg_basebackup.
- Синхронизация реплик выполняется PostgreSQL; приложение использует primary для записи и при наличии — read replica для чтения (пул уже создаётся при заданном `DATABASE_READ_REPLICA_URL`).

---

**Статус:** реализованы поддержка кластера, координация узлов через Redis, health с nodeId, опциональный пул read replica и документация по балансировщику и репликации.
