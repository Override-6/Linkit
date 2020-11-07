package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.TaskInitPacket
import fr.overridescala.vps.ftp.api.task.ext.TaskExtension

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * handles TaskCompleters from
 * an initialization DataPacket
 * */
class TaskCompleterHandler {

    private val completers: mutable.Map[String, TaskInitPacket => TaskExecutor] = new mutable.HashMap()
    private val families: mutable.Map[Class[_ <: TaskExtension], ListBuffer[String]] = new mutable.HashMap()

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
        val targetID = initPacket.targetID
        val senderID = initPacket.senderID
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
    def putCompleter(taskType: String, supplier: TaskInitPacket => TaskExecutor)(implicit extension: TaskExtension): Unit = {
        completers.put(taskType, supplier)
        families.getOrElseUpdate(extension.getClass, ListBuffer.empty)
                .addOne(taskType)
    }

    def getLoadedTasks(extension: Class[_ <: TaskExtension]): Array[String] =
        families.getOrElseUpdate(extension, ListBuffer.empty)
                .toArray

    def isRegistered(taskType: String): Boolean =
        completers.contains(taskType)



}
