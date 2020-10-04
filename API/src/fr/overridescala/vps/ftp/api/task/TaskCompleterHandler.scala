package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet.TaskInitPacket

/**
 * handles TaskCompleters from
 * an initialization DataPacket
 * */
trait TaskCompleterHandler {
    //TODO edit doc

    /**
     * @param initPacket the initialization packet where the header is the targeted task type, and content is the arguments to initialise the completer.
     * @param ownerID the task owner identifier, the Relay that is at the origin of this task
     * @throws TaskException if no completer where found from this packet header
     *
     * @see [[TaskInitPacket]]
     * */
    def handleCompleter(initPacket: TaskInitPacket, ownerID: String, tasksHandler: TasksHandler): Unit

    /**
     * To be extensible, the user need to add Completer suppliers to handlers in order to inject his own tasks into the program.
     * @param taskType the task type for what the supplier will be called.
     * @param supplier this lambda takes a DataPacket (wish is the Initialization packet) the Tasks Handler
     *                 and the task owner identifier
     * */
    def putCompleter(taskType: String, supplier: (TaskInitPacket, TasksHandler, String) => Unit)

}
