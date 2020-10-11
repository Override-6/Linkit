package fr.overridescala.vps.ftp.api.task.ext

import fr.overridescala.vps.ftp.api.task.TasksHandler

abstract class TaskExtension(protected val tasksHandler: TasksHandler) {
    def main(): Unit
}