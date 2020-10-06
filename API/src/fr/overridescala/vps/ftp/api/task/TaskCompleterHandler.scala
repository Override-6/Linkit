package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet.TaskInitPacket

/**
 * handles TaskCompleters from
 * an initialization DataPacket
 * */
trait TaskCompleterHandler {
    /**
     * @param initPacket the initialization packet for completer.
     * @param tasksHandler the handler that will schedule the completer
     * @throws TaskException if no completer where found from this packet header
     *
     * @see [[TaskInitPacket]]
     * */
    def handleCompleter(initPacket: TaskInitPacket, tasksHandler: TasksHandler): Unit

    /**
     * To be extensible, the user need to add Completer suppliers to handlers in order to inject his own tasks into the program.
     * @param taskType the task type for what the supplier will be called.
     * @param supplier this lambda takes a [[TaskInitPacket]] the Tasks Handler and the init packet sender identifier
     *                 and the task owner identifier
     * */
    def putCompleter(taskType: String, supplier: (TaskInitPacket, TasksHandler) => Unit)

}
