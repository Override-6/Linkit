package fr.overridescala.vps.ftp.api.task

class TaskTicket(private val taskExecutor: TaskExecutor,
                 private val sessionID: Int,
                 private val ownFreeWill: Boolean) {

    val taskName: String = taskExecutor.getClass.getSimpleName

    def start(): Unit = {
        try {
            println(s"executing $taskName...")
            if (ownFreeWill)
                taskExecutor.sendTaskInfo()
            taskExecutor.execute()
        } catch {
            case e: Throwable => e.printStackTrace()
        }
    }

    override def toString: String =
        s"Ticket(name = $taskName," +
                s" id = $sessionID," +
                s" freeWill = $ownFreeWill)"

}
