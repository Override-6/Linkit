package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.packet.DataPacket

/**
 * Creates a TaskCompleter from
 * an initialization DataPacket
 * */
trait TaskCompleterFactory {

    /**
     * @param initPacket the initialization packet where the header is the targeted task type, and content is the arguments to initialise the completer.
     * @return the TaskExecutor in connection to the initialization packet
     * @throws NoSuchElementException if no completer where found from this packet header
     *
     * @see [[DataPacket]]
     * */
    def getCompleter(initPacket: DataPacket): TaskExecutor

    /**
     * To be extensible, the user need to add Completer suppliers to factories in order to inject his own tasks into the program.
     * @param taskType the task type for what the supplier will be called.
     * @param supplier this lambda takes a DataPacket (wish is the Initialization packet) and the Tasks Handler then return a TaskExecutor
     * */
    def putCompleter(taskType: String, supplier: (DataPacket, TasksHandler) => TaskExecutor)

}
