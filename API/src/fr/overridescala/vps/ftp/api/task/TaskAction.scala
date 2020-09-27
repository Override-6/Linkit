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
     * @param identifier specifies the task identifier used for packet channels.
     * */
    def queue(onSuccess: T => Unit = _, onError: String => Unit = _, identifier: Int = _): Unit

    /**
     * Completes the task. That does not mean that this task is not enqueued.
     * The particularity of this method, is that it will wait until the task end.
     * If the task was unsuccessful, throw an error, return the result instead.
     *
     * @return the task result
     * */
    def complete(identifier: Int = _): T

}
