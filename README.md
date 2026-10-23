# wise-task-gateway

Единая точка входа платформы WiseTask. Принимает запросы фронтенда по GraphQL, проверяет JWT-авторизацию и роутит вызовы в микросервисы по gRPC. Здесь же живёт выдача и обновление токенов (sign up / sign in / refresh / сброс пароля).

## Технологии

- Java 25, Spring Boot 4.1.x
- Spring for GraphQL (endpoint `/graphql`, IDE — `/graphiql`)
- Spring gRPC clients (profile, task, plugin, graph, event)
- Spring Security + JWT (RS256, ключи в `src/main/resources/certs/`)
- Redis — кэши профилей, задач, графов, плагинов, решений
- Actuator + Micrometer: метрики для Prometheus, логи в Loki, трейсы в Tempo

## Архитектура

```
Frontend ──HTTP/GraphQL──> gateway ──gRPC──> wise-task-profile  :6567
                                      ├──gRPC──> wise-task-task    :6568
                                      ├──gRPC──> wise-task-plugin  :6566
                                      ├──gRPC──> wise-task-graph   :6565
                                      └──gRPC──> wise-task-event   :6569 (статистика)
```

- `controller/*Controller` — GraphQL-резолверы запросов и мутаций (`schema.graphqls` и файлы в `resources/graphql/`).
- `controller/*SchemaMapper` — резолверы вложенных полей (например, `author` у задачи/графа/плагина).
- `service/grpc/*GrpcService` — обёртки над gRPC-стабами.
- `service/*Service` — бизнес-логика gateway: кэширование, обогащение данными авторов.
- Роли: `USER`, `AUTHOR`, `ADMIN` — доступ регулируется `@PreAuthorize` на контроллерах.

## API

GraphQL-схема: `src/main/resources/graphql/schema.graphqls`. Основные группы:

- **Профиль** — `signUp`, `signIn`, `refreshToken`, `resetPassword`, `changePassword`, `updateProfile`, `deleteProfile`, запросы `getMyProfile`, `getAllProfiles`.
- **Графы** — `createGraph`, `generateGraph`, `deleteGraph`, `getGraphById`, `getAllGraphs` (фильтры + пагинация).
- **Плагины** — `createPlugin`, `updatePlugin`, `deletePlugin`, `checkPluginSolution`, `checkPluginImplementation`, `validatePlugin`, `getAllPlugins`.
- **Задачи** — `createTaskGraph/Implementation`, `updateTaskGraph/Implementation`, `solveTaskGraph/Implementation`, `deleteTask`, `getTask`, `getAllTasks`.
- **Решения и статистика** — `getTaskSolution`, `getAllTaskSolutions`, `getStatistic`.

## Конфигурация

Переменные окружения (значения по умолчанию — в `example.env`):

| Переменная | Назначение |
|---|---|
| `GATEWAY_PORT` | HTTP-порт (по умолчанию 8084) |
| `JWT_ACCESS_EXPIRES_AT` / `JWT_REFRESH_EXPIRES_AT` | Время жизни токенов |
| `CORS_ALLOWED_ORIGINS` | Разрешённые источники фронта |
| `*_APP_NAME`, `*_GRPC_PORT` | Адреса микросервисов (profile/task/plugin/graph/event) |
| `DB_HOST`, `REDIS_PORT`, `TEMPO_HOST`, `LOKI_HOST` | Инфраструктура |

JWT-ключи: `src/main/resources/certs/{public,private}.pem` (можно переопределить через `security.jwt.public-key/private-key`).

## Запуск

Сервис — корневая точка docker-стенда: в `docker-compose.yaml` описаны все сервисы вместе с инфраструктурой (postgres, mongo, redis, smtp, grafana/loki/prometheus/tempo).

```bash
cp example.env .env   # при первом запуске
docker compose up -d --build wise-task-gateway
```

GraphiQL: `http://localhost:8084/graphiql`. Здоровье: `http://localhost:8084/actuator/health`.

Локально без docker: `./gradlew bootRun` (нужны поднятые сервисы и БД, адреса через env).
