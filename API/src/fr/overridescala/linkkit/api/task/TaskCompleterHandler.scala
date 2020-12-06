package fr.overridescala.linkkit.api.task

import fr.overridescala.linkkit.api.`extension`.RelayExtension
import fr.overridescala.linkkit.api.exceptions.TaskException
import fr.overridescala.linkkit.api.packet.PacketCoordinates
import fr.overridescala.linkkit.api.packet.fundamental.TaskInitPacket
import fr.overridescala.linkkit.api.`extension`.RelayExtension
import fr.overridescala.linkkit.api.exceptions.TaskException
import fr.overridescala.linkkit.api.packet.PacketCoordinates
import fr.overridescala.linkkit.api.packet.fundamental.TaskInitPacket

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * handles TaskCompleters from
 * an initialization DataPacket
 * */
class TaskCompleterHandler {

    private val completers: mutable.Map[String, TaskInitPacket => TaskExecutor] = new mutable.HashMap()
    private val families: mutable.Map[Class[_ <: RelayExtension], ListBuffer[String]] = new mutable.HashMap()

    /**
     * @param initPacket the initialization packet for completer.
     * @param tasksHandler the handler that will schedule the completer
     * @throws TaskException if no completer where found from this packet header
     *
     * @see [[TaskInitPacket]]
     * */
    def handleCompleter(initPacket: TaskInitPacket, coords: PacketCoordinates, tasksHandler: TasksHandler): Unit = {
        val taskType = initPacket.taskType
        val taskID = coords.channelID
        val targetID = coords.targetID
        val senderID = coords.senderID
        val completerOpt = completers.get(taskType)
        if (completerOpt.isEmpty)
            throw new TaskException(s"Could not find completer of type '$taskType'")

        val completer = completerOpt.get.apply(initPacket)
        tasksHandler.registerTask(completer, taskID, targetID, senderID, false)
    }

    /**
     * To be extensible, the user need to add Completer suppliers to handlers in order to inject his own tasks into the program.
     * @param taskType the task type for what the supplier will be called.
     * @param supplier this lambda takes a [[TaskInitPacket]] the Tasks Handler and the init packet sender identifier
     *                 and the task owner identifier
     * */
    def putCompleter(taskType: String, supplier: TaskInitPacket => TaskExecutor)(implicit extension: RelayExtension): Unit = {
        completers.put(taskType, supplier)
        families.getOrElseUpdate(extension.getClass, ListBuffer.empty)
                .addOne(taskType)
    }

    def getLoadedTasks(extension: Class[_ <: RelayExtension]): Array[String] =
        families.getOrElseUpdate(extension, ListBuffer.empty)
                .toArray

    def isRegistered(taskType: String): Boolean =
        completers.contains(taskType)


}
