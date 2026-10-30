package ru.ithub.tasks.model

import kotlinx.serialization.Serializable

@Serializable
enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
}

@Serializable
data class Task(
    val id: Int,
    val title: String,
    val done: Boolean,
    val priority: Priority,
)

@Serializable
data class CreateTaskRequest(
    val title: String,
    val done: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
)

@Serializable
data class ServiceInfo(
    val name: String,
    val description: String,
)

sealed class ApiException(message: String) : RuntimeException(message)

class BadRequestException(message: String) : ApiException(message)

class NotFoundException(message: String) : ApiException(message)

class ConflictException(message: String) : ApiException(message)
