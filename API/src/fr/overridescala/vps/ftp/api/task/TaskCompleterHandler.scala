package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.TaskInitPacket

import scala.collection.mutable

/**
 * handles TaskCompleters from
 * an initialization DataPacket
 * */
class TaskCompleterHandler {

    private val completers: mutable.Map[String, TaskInitPacket => TaskExecutor] = new mutable.HashMap()

    /**
     * @param initPacket the initialization packet for completer.
     * @param tasksHandler the handler that will schedule the completer
     * @throws TaskException if no completer where found from this packet header
     *
     * @see [[TaskInitPacket]]
     * */
    def handleCompleter(initPacket: TaskInitPacket, tasksHandler: TasksHandler): Unit = {
        val taskType = initPacket.taskType
        val taskID = initPacket.channelID
        val targetID = initPacket.targetIdentifier
        val senderID = initPacket.senderIdentifier
        val supplierOpt = completers.get(taskType)
        if (supplierOpt.isEmpty)
            throw new TaskException(s"Could not find completer of type '$taskType'")
        val completer = supplierOpt.get.apply(initPacket)
        tasksHandler.registerTask(completer, taskID, targetID, senderID, false)
    }

    /**
     * To be extensible, the user need to add Completer suppliers to handlers in order to inject his own tasks into the program.
     * @param taskType the task type for what the supplier will be called.
     * @param supplier this lambda takes a [[TaskInitPacket]] the Tasks Handler and the init packet sender identifier
     *                 and the task owner identifier
     * */
    def putCompleter(taskType: String, supplier: TaskInitPacket => TaskExecutor): Unit =
        completers.put(taskType, supplier)



}
