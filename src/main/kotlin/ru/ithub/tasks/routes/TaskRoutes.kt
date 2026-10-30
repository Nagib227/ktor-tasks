package ru.ithub.tasks.routes

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import ru.ithub.tasks.model.BadRequestException
import ru.ithub.tasks.model.CreateTaskRequest
import ru.ithub.tasks.model.NotFoundException
import ru.ithub.tasks.model.Priority
import ru.ithub.tasks.model.ServiceInfo
import ru.ithub.tasks.storage.TaskRepository

fun Application.configureRouting(repository: TaskRepository) {
    routing {
        get("/") {
            call.respond(
                ServiceInfo(
                    name = "ktor-tasks",
                    description = "REST API задач",
                ),
            )
        }
        route("/tasks") {
            get {
                val done = call.optionalBoolean("done")
                val priority = call.optionalPriority("priority")
                val query = call.request.queryParameters["q"]?.trim()?.takeIf { it.isNotEmpty() }
                call.respond(repository.list(done, priority, query))
            }
            get("/{id}") {
                val id = call.requireTaskId()
                val task = repository.find(id) ?: throw NotFoundException("Задача с id=$id не найдена")
                call.respond(task)
            }
            post {
                val request = call.receive<CreateTaskRequest>()
                val created = repository.create(request)
                call.response.header(HttpHeaders.Location, "/tasks/${created.id}")
                call.respond(HttpStatusCode.Created, created)
            }
            delete("/{id}") {
                val id = call.requireTaskId()
                if (!repository.delete(id)) {
                    throw NotFoundException("Задача с id=$id не найдена")
                }
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun ApplicationCall.optionalBoolean(name: String): Boolean? {
    val raw = request.queryParameters[name] ?: return null
    return when (raw.lowercase()) {
        "true" -> true
        "false" -> false
        else -> throw BadRequestException("Параметр $name должен быть true или false")
    }
}

private fun ApplicationCall.optionalPriority(name: String): Priority? {
    val raw = request.queryParameters[name] ?: return null
    if (raw.isBlank()) {
        throw BadRequestException("Параметр $name должен быть LOW, MEDIUM или HIGH")
    }
    return enumValues<Priority>().firstOrNull { it.name.equals(raw, ignoreCase = true) }
        ?: throw BadRequestException("Параметр $name должен быть LOW, MEDIUM или HIGH")
}

private fun ApplicationCall.requireTaskId(): Int {
    val id = parameters["id"]?.toIntOrNull()
    if (id == null || id <= 0) {
        throw BadRequestException("Параметр path id должен быть положительным целым числом")
    }
    return id
}
