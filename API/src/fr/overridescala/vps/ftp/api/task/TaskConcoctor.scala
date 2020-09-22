package fr.overridescala.vps.ftp.api.task

/**
 * Functional interface allows the user to pre-instantiate the task
 * without handling all the internal stuff needed for really instantiate the task
 * Concoctors are only needed by non-TaskCompleters.
 * @see [[Task]]
 * @see [[fr.overridescala.vps.ftp.api.Relay]]
 * */
@FunctionalInterface
trait TaskConcoctor[R, T >: TaskAction[R]] {

    /**
     * concoct the Task
     * @param tasksHandler the tasksHandler used by [[Task]]
     * @return the task instance
     * */
    def concoct(tasksHandler: TasksHandler): T

}
