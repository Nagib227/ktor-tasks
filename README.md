# Сервер задач на Ktor

Учебный REST-сервер со списком задач. Данные хранятся в памяти процесса и сбрасываются при перезапуске. При старте уже есть три задачи.

## Требования

- JDK 17 или новее
- Интернет при первом запуске: Gradle скачает зависимости

## Запуск

В каталоге проекта:

```bat
gradlew.bat run
```

На Linux и macOS:

```bash
./gradlew run
```

Сервер слушает [http://localhost:8080](http://localhost:8080). Другой порт задаётся переменной `PORT`.

Остановка: `Ctrl+C` в том же терминале.

Проверка сборки без запуска сервера:

```bat
gradlew.bat test
```

## Маршруты

| Метод | Путь | Что делает |
| --- | --- | --- |
| GET | `/tasks` | Список задач |
| GET | `/tasks/{id}` | Одна задача |
| POST | `/tasks` | Создать задачу |
| DELETE | `/tasks/{id}` | Удалить задачу |

JSON подключён через `ContentNegotiation` и `kotlinx.serialization`.

### Параметры query у `GET /tasks`

Все параметры необязательны и объединяются по «и».

| Параметр | Пример | Смысл |
| --- | --- | --- |
| `done` | `true` или `false` | Фильтр по статусу |
| `priority` | `LOW`, `MEDIUM`, `HIGH` | Фильтр по приоритету, регистр не важен |
| `q` | `ktor` | Поиск подстроки в названии, регистр не важен |

Параметр path `id` — положительное целое число.

### Тело `POST /tasks`

```json
{
  "title": "Проверить API",
  "done": false,
  "priority": "LOW"
}
```

`title` обязателен. `done` по умолчанию `false`, `priority` по умолчанию `MEDIUM`. Название обрезается по краям, не длиннее 200 символов. Повтор названия без учёта регистра возвращает `409`.

## Коды ответов

| Код | Когда |
| --- | --- |
| 200 | Список или одна задача |
| 201 | Задача создана. Заголовок `Location` указывает на неё |
| 204 | Задача удалена, тела нет |
| 400 | Пустое название, неверный query или path, битый JSON |
| 404 | Нет задачи или нет такого маршрута |
| 409 | Задача с таким названием уже есть |
| 415 | У `POST` не `Content-Type: application/json` |

Ошибка — это JSON:

```json
{
  "error": "not_found",
  "message": "Задача с id=99 не найдена"
}
```

Поле `error`: `bad_request`, `not_found`, `conflict`, `unsupported_media_type`.

## Примеры

PowerShell:

```powershell
curl.exe http://localhost:8080/tasks
curl.exe "http://localhost:8080/tasks?done=false&priority=HIGH&q=ktor"
curl.exe http://localhost:8080/tasks/1
curl.exe -X POST http://localhost:8080/tasks -H "Content-Type: application/json" -d '{"title":"Проверить API","priority":"LOW"}'
curl.exe -X DELETE http://localhost:8080/tasks/1
```

Linux и macOS:

```bash
curl http://localhost:8080/tasks
curl "http://localhost:8080/tasks?done=false&priority=HIGH&q=ktor"
curl http://localhost:8080/tasks/1
curl -X POST http://localhost:8080/tasks -H "Content-Type: application/json" -d '{"title":"Проверить API","priority":"LOW"}'
curl -X DELETE http://localhost:8080/tasks/1
```

## Стартовые данные

| id | Название | done | priority |
| --- | --- | --- | --- |
| 1 | Изучить маршруты Ktor | false | HIGH |
| 2 | Подключить JSON-сериализацию | true | MEDIUM |
| 3 | Написать README | false | LOW |

Следующая созданная задача получит id `4`. После удаления номер не переиспользуется.

## Структура

```text
ktor-tasks/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew / gradlew.bat
├── README.md
└── src/main/kotlin/ru/ithub/tasks/
    ├── Application.kt          # запуск, ContentNegotiation, коды ошибок
    ├── routes/TaskRoutes.kt    # GET, POST, DELETE, query и path
    ├── storage/TaskRepository.kt
    └── model/Models.kt
```
