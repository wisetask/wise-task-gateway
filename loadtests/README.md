# Нагрузочные тесты wise-task-gateway (k6)

Нагрузочный стенд на [k6](https://k6.io), запуск через официальный docker-образ `grafana/k6` — на хосте нужен только Docker и Node (для сравнения результатов). Скрипты тестируют GraphQL-эндпоинт `POST /graphql` и actuator-ручки шлюза, эмулируя поведение реального фронтенда.

## Запуск

```bash
cd wise-task-gateway/loadtests
./run.sh --url=http://localhost:8084 --email=user@example.com --password=secret
```

`run.sh` монтирует каталог в контейнер, пробрасывает переменные (`--network host` — доступ к шлюзу на localhost) и сохраняет JSON-отчёт в `results/`. Имя образа можно переопределить: `K6_IMAGE=grafana/k6:0.57.0 ./run.sh ...`.

Без учётных данных аутентифицированные операции исключаются из микса — тестируются только публичные ручки (`/actuator/health`, `/actuator/prometheus`, неавторизованный доступ).

Оффлайн-проверка скриптов без шлюза (mock-эмуляция на Node):

```bash
node mock-server.js 8099 &
./run.sh --url=http://127.0.0.1:8099 --email=loadtest@example.com --password=Password123 --vus=5 --duration=10s --no-ramp
kill %1
```

## Профиль нагрузки

По умолчанию, для авторизованного пользователя:

| Операция | Доля | Бэкенд шлюза |
|---|---|---|
| getAllTasks | 20% | task (gRPC) |
| getAllGraphs, getGraphById | 20% | graph (gRPC) |
| getAllPlugins, getPlugin | 15% | plugin (gRPC) |
| getMyProfile, getAllProfiles | 10% | profile (gRPC) |
| getAllTaskSolutions | 5% | task (gRPC) |
| getStatistic (SUM/USER, SUCCESS_RATE/TASK) | 10% | statistics (gRPC) |
| generateGraph (isSaved=false) | 10% | graph (gRPC) |
| signIn, refreshToken | 4% | profile (gRPC) + JWT |
| getMyProfile без токена | 3% | security-путь |
| actuator health/prometheus | 7% | сам шлюз |

Опциональные сценарии (выключены по умолчанию):

- `--writes=1` — пары createGraph+deleteGraph как одна операция (возвращённый граф сразу удаляется — данные не накапливаются), weight 3
- `--sign-up=1` — создание профилей loadtest-* (пишет в БД профилей!), weight 1
- `--solve-task-id=<id>` — solveTaskGraph по указанной задаче (тяжёлая операция: task → plugin → graph), weight 2

## Параметры

Все параметры передаются через `run.sh` (под капотом — env-переменные `LOADTEST_*` / `GATEWAY_URL` для k6):

| Флаг | Env | По умолчанию | Описание |
|---|---|---|---|
| `--url` | `GATEWAY_URL` | `http://localhost:8084` | Целевой шлюз |
| `--vus` | `LOADTEST_VUS` | 10 | Виртуальные пользователи в плато |
| `--duration` | `LOADTEST_DURATION` | 60s | Длительность плато |
| `--ramp-up` | `LOADTEST_RAMP_UP` | 15s | Линейный разгон 0→vus |
| `--ramp-down` | `LOADTEST_RAMP_DOWN` | 10s | Линейный спуск vus→0 |
| `--no-ramp` | `LOADTEST_NO_RAMP=1` | — | Без разгона/спуска |
| `--timeout` | `LOADTEST_TIMEOUT` | 30s | Таймаут запроса |
| `--think-time` / `--think-time-jitter` | `LOADTEST_THINK_TIME(_JITTER)` | 0 | Пауза между операциями у VU (сек или `250ms`) |
| `--email` / `--password` | `LOADTEST_SIGN_IN_EMAIL` / `LOADTEST_SIGN_IN_PASSWORD` | — | Учётка тестового пользователя |
| `--graph-vertices` / `--graph-edges` | `LOADTEST_GRAPH_VERTICES` / `LOADTEST_GRAPH_EDGES` | 20 / 20 | Размер графов в generateGraph/createDeleteGraph |
| `--label` | `LOADTEST_LABEL` | — | Метка версии/прогона (попадает в имя файла и отчёт) |
| `--out` | `LOADTEST_OUT` | `results/loadtest-<stamp>.json` | Путь JSON-отчёта |

Пример: ступенчатый тест на поиск предела

```bash
./run.sh --url=http://localhost:8084 --email=u@e.com --password=p --vus=50 --duration=2m --ramp-up=30s
```

## Результаты

- Таблица в stdout: requests, ok/fail/expected, RPS, p50/p90/p95/p99/max по каждой операции
- JSON-файл в `results/` (мета о версии шлюза из `/actuator/info`, конфиг, per-op статистика)
- `expected` — ожидаемые ошибки (неавторизованный доступ, signIn без учётки), не считаются отказами
- Threshold `http_req_failed < 50%`: при большем числе HTTP-ошибок k6 завершится с exit 1

Warmup в k6 не выносится отдельным этапом: при необходимости сначала выполните короткий прогон и не учитывайте его результаты.

## Сравнение версий приложения

```bash
git checkout v2.0.0 && (cd .. && ./gradlew :wise-task-gateway:bootRun)   # или docker compose up
./run.sh --label=v2.0.0 --email=... --password=...

git checkout v2.1.0 && ...
./run.sh --label=v2.1.0 --email=... --password=...

node compare-results.js results/loadtest-...-v2.0.0.json results/loadtest-...-v2.1.0.json
```

`compare-results.js` выводит дельты RPS, p95 и доли ошибок по операциям; падение RPS или рост p95/ошибок сверх порога (по умолчанию 15%, `--threshold=10`) помечается `REGRESSION`, exit code 1 — можно встроить в CI. `--no-fail` — только отчёт.

Для корректного сравнения держите одинаковыми: `--vus`, `--duration`, флаги writes/sign-up, данные на стенде и размер графов.

## Структура

| Файл | Назначение |
|---|---|
| `run.sh` | docker-обёртка: монтирование, env, сеть host |
| `load-test.js` | k6-скрипт: setup (auth, sample-ids), взвешенный выбор операций, handleSummary |
| `operations.js` | операции из реальных GraphQL-схем шлюза с весами |
| `config.js` | env-конфигурация и построение stages |
| `compare-results.js` | сравнение JSON-отчётов двух прогонов (Node, без зависимостей) |
| `mock-server.js` | эмуляция шлюза для оффлайн-проверки скриптов (Node) |

## Важно

- Не запускайте против продового стенда wisetask.ru:2235 — только локальный/docker-стенд.
- `--writes=1` не накапливает данные (пары create+delete), `--sign-up=1` создаёт профили в БД.
- Тестовый пользователь должен существовать на стенде (или создайте его заранее через signUp).
