package ru.ithub.tasks

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConvertException
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import ru.ithub.tasks.model.ConflictException
import ru.ithub.tasks.model.ErrorResponse
import ru.ithub.tasks.model.NotFoundException
import ru.ithub.tasks.routes.configureRouting
import ru.ithub.tasks.storage.TaskRepository
import ru.ithub.tasks.model.BadRequestException as ApiBadRequestException

private const val INVALID_JSON =
    "Некорректное JSON-тело. Ожидается объект с полем title и необязательными done и priority"

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull()?.takeIf { it in 1..65535 } ?: 8080
    embeddedServer(Netty, port = port, host = "127.0.0.1") {
        module()
    }.start(wait = true)
}

fun Application.module(repository: TaskRepository = TaskRepository.withSamples()) {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            },
        )
    }
    install(StatusPages) {
        exception<ApiBadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", cause.message ?: "Некорректный запрос"))
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", cause.message ?: "Не найдено"))
        }
        exception<ConflictException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse("conflict", cause.message ?: "Конфликт"))
        }
        exception<BadRequestException> { call, cause ->
            val message = if (cause.hasSerializationCause()) INVALID_JSON else cause.message ?: "Некорректный запрос"
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", message))
        }
        exception<SerializationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", INVALID_JSON))
        }
        exception<ContentConvertException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", INVALID_JSON))
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Маршрут не найден"))
        }
        status(HttpStatusCode.UnsupportedMediaType) { call, _ ->
            call.respond(
                HttpStatusCode.UnsupportedMediaType,
                ErrorResponse("unsupported_media_type", "Ожидается Content-Type: application/json"),
            )
        }
    }
    configureRouting(repository)
    log.info("Маршруты задач зарегистрированы")
}

private fun Throwable.hasSerializationCause(): Boolean {
    val seen = HashSet<Throwable>()
    var current: Throwable? = this
    while (current != null && seen.add(current)) {
        if (current is SerializationException || current is ContentConvertException) {
            return true
        }
        current = current.cause
    }
    return false
}
