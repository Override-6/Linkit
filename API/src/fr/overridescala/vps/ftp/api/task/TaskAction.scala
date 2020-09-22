package fr.overridescala.vps.ftp.api.task

/**
 * The usable side for a user to handle this Task.
 * this trait can only enqueue, or complete the task
 *
 * @see [[Task]]
 * @see [[TaskAction]]
 * */
trait TaskAction[T] {

    /**
     * Enqueue / register this task.
     * The task will be executed after all task before are ended.
     * @param onSuccess the action to perform when the task was successful
     * @param onError the action to perform when the task was unsuccessful
     * */
    def queue(onSuccess: T => Unit = t => {}, onError: String => Unit = Console.err.println): Unit

    /**
     * Completes the task. That does not mean that this task is not enqueued.
     * The particularity of this method, is that it will wait until the task end.
     * If the task was unsuccessful, throw an error, return the result instead.
     *
     * @return the task result
     * */
    def complete(): T

}
