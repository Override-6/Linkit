package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.packet.DataPacket

/**
 * handles TaskCompleters from
 * an initialization DataPacket
 * */
trait TaskCompleterHandler {

    /**
     * @param initPacket the initialization packet where the header is the targeted task type, and content is the arguments to initialise the completer.
     * @param ownerID the task owner identifier, the Relay that is at the origin of this task
     * @throws NoSuchElementException if no completer where found from this packet header
     *
     * @see [[DataPacket]]
     * */
    def handleCompleter(initPacket: DataPacket, ownerID: String): Unit

    /**
     * To be extensible, the user need to add Completer suppliers to factories in order to inject his own tasks into the program.
     * @param taskType the task type for what the supplier will be called.
     * @param supplier this lambda takes a DataPacket (wish is the Initialization packet) the Tasks Handler
     *                 and the task owner identifier then return a TaskExecutor
     * */
    def putCompleter(taskType: String, supplier: (DataPacket, TasksHandler, String) => TaskExecutor)

}
