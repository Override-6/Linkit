package fr.overridescala.vps.ftp.api.task

@FunctionalInterface
trait TaskConcoctor[R, T >: TaskAction[R]] {

    def concoct(tasksHandler: TasksHandler): T

}
