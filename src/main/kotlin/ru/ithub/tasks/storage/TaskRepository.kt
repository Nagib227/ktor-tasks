package ru.ithub.tasks.storage

import ru.ithub.tasks.model.BadRequestException
import ru.ithub.tasks.model.ConflictException
import ru.ithub.tasks.model.CreateTaskRequest
import ru.ithub.tasks.model.Priority
import ru.ithub.tasks.model.Task

/** Потокобезопасное хранилище задач в памяти процесса. */
class TaskRepository(seed: List<Task> = emptyList()) {
    private val tasks = seed.toMutableList()
    private var nextId = (seed.maxOfOrNull { it.id } ?: 0) + 1

    fun list(done: Boolean?, priority: Priority?, query: String?): List<Task> = synchronized(tasks) {
        tasks
            .filter { task ->
                (done == null || task.done == done) &&
                    (priority == null || task.priority == priority) &&
                    (query == null || task.title.contains(query, ignoreCase = true))
            }
            .sortedBy { it.id }
    }

    fun find(id: Int): Task? = synchronized(tasks) {
        tasks.find { it.id == id }
    }

    fun create(request: CreateTaskRequest): Task = synchronized(tasks) {
        val title = request.title.trim()
        if (title.isEmpty()) {
            throw BadRequestException("Название задачи не может быть пустым")
        }
        if (title.length > MAX_TITLE_LENGTH) {
            throw BadRequestException("Название задачи не длиннее $MAX_TITLE_LENGTH символов")
        }
        if (tasks.any { it.title.equals(title, ignoreCase = true) }) {
            throw ConflictException("Задача с названием «$title» уже существует")
        }
        val task = Task(
            id = nextId++,
            title = title,
            done = request.done,
            priority = request.priority,
        )
        tasks += task
        task
    }

    fun delete(id: Int): Boolean = synchronized(tasks) {
        tasks.removeAll { it.id == id }
    }

    companion object {
        const val MAX_TITLE_LENGTH = 200

        fun withSamples(): TaskRepository = TaskRepository(
            listOf(
                Task(id = 1, title = "Изучить маршруты Ktor", done = false, priority = Priority.HIGH),
                Task(id = 2, title = "Подключить JSON-сериализацию", done = true, priority = Priority.MEDIUM),
                Task(id = 3, title = "Написать README", done = false, priority = Priority.LOW),
            ),
        )
    }
}
