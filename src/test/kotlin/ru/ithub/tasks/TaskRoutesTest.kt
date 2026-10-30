package ru.ithub.tasks

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import ru.ithub.tasks.model.ErrorResponse
import ru.ithub.tasks.model.Priority
import ru.ithub.tasks.model.Task
import ru.ithub.tasks.storage.TaskRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `list returns seed tasks as json`() = testApplication {
        application { module() }
        val response = client.get("/tasks")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.headers[HttpHeaders.ContentType].orEmpty().contains("application/json"))
        val tasks = json.decodeFromString<List<Task>>(response.bodyAsText())
        assertEquals(listOf(1, 2, 3), tasks.map { it.id })
    }

    @Test
    fun `query filters by done priority and search together`() = testApplication {
        application { module() }
        val response = client.get("/tasks?done=false&priority=HIGH&q=ktor")
        assertEquals(HttpStatusCode.OK, response.status)
        val tasks = json.decodeFromString<List<Task>>(response.bodyAsText())
        assertEquals(1, tasks.size)
        assertEquals("Изучить маршруты Ktor", tasks.single().title)
        assertEquals(Priority.HIGH, tasks.single().priority)
    }

    @Test
    fun `search is case insensitive and empty result is an empty array`() = testApplication {
        application { module() }
        val found = client.get("/tasks?q=readme")
        val tasks = json.decodeFromString<List<Task>>(found.bodyAsText())
        assertEquals(listOf(3), tasks.map { it.id })

        val missing = client.get("/tasks?q=неттакого")
        assertEquals(emptyList(), json.decodeFromString<List<Task>>(missing.bodyAsText()))
    }

    @Test
    fun `invalid query returns 400 json`() = testApplication {
        application { module() }
        val done = client.get("/tasks?done=yes")
        assertEquals(HttpStatusCode.BadRequest, done.status)
        assertEquals("bad_request", json.decodeFromString<ErrorResponse>(done.bodyAsText()).error)

        val priority = client.get("/tasks?priority=urgent")
        assertEquals(HttpStatusCode.BadRequest, priority.status)
        assertTrue(json.decodeFromString<ErrorResponse>(priority.bodyAsText()).message.contains("priority"))
    }

    @Test
    fun `path id returns task or 404 or 400`() = testApplication {
        application { module() }
        val found = client.get("/tasks/2")
        assertEquals(HttpStatusCode.OK, found.status)
        assertEquals(2, json.decodeFromString<Task>(found.bodyAsText()).id)

        val missing = client.get("/tasks/99")
        assertEquals(HttpStatusCode.NotFound, missing.status)
        assertEquals("not_found", json.decodeFromString<ErrorResponse>(missing.bodyAsText()).error)

        val invalid = client.get("/tasks/abc")
        assertEquals(HttpStatusCode.BadRequest, invalid.status)
        assertEquals("bad_request", json.decodeFromString<ErrorResponse>(invalid.bodyAsText()).error)
    }

    @Test
    fun `post creates task and returns 201`() = testApplication {
        application { module(TaskRepository()) }
        val response = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"  Проверить API  ","priority":"LOW"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.headers[HttpHeaders.ContentType].orEmpty().contains("application/json"))
        val task = json.decodeFromString<Task>(response.bodyAsText())
        assertEquals(1, task.id)
        assertEquals("Проверить API", task.title)
        assertEquals(false, task.done)
        assertEquals(Priority.LOW, task.priority)
        assertEquals("/tasks/1", response.headers[HttpHeaders.Location])

        val loaded = client.get("/tasks/1")
        assertEquals(task, json.decodeFromString<Task>(loaded.bodyAsText()))
    }

    @Test
    fun `post uses defaults when optional fields are omitted`() = testApplication {
        application { module(TaskRepository()) }
        val response = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Только название"}""")
        }
        val task = json.decodeFromString<Task>(response.bodyAsText())
        assertEquals(false, task.done)
        assertEquals(Priority.MEDIUM, task.priority)
    }

    @Test
    fun `post rejects blank title invalid json and duplicate title`() = testApplication {
        application { module() }
        val blank = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"   "}""")
        }
        assertEquals(HttpStatusCode.BadRequest, blank.status)

        val broken = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":""")
        }
        assertEquals(HttpStatusCode.BadRequest, broken.status)
        assertEquals("bad_request", json.decodeFromString<ErrorResponse>(broken.bodyAsText()).error)

        val duplicate = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"  написать readme  "}""")
        }
        assertEquals(HttpStatusCode.Conflict, duplicate.status)
        val error = json.decodeFromString<ErrorResponse>(duplicate.bodyAsText())
        assertEquals("conflict", error.error)
        assertTrue(error.message.contains("уже существует"))
    }

    @Test
    fun `delete removes task and missing id returns 404`() = testApplication {
        application { module() }
        val deleted = client.delete("/tasks/1")
        assertEquals(HttpStatusCode.NoContent, deleted.status)
        assertEquals("", deleted.bodyAsText())

        val missing = client.get("/tasks/1")
        assertEquals(HttpStatusCode.NotFound, missing.status)

        val second = client.delete("/tasks/1")
        assertEquals(HttpStatusCode.NotFound, second.status)

        val invalid = client.delete("/tasks/0")
        assertEquals(HttpStatusCode.BadRequest, invalid.status)
    }

    @Test
    fun `unknown route returns 404 json`() = testApplication {
        application { module() }
        val response = client.get("/missing")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("not_found", json.decodeFromString<ErrorResponse>(response.bodyAsText()).error)
    }
}
