package fr.overridescala.vps.ftp.server

import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketChannel}
import fr.overridescala.vps.ftp.api.task.tasks.{DownloadTask, FileInfoTask, UploadTask}
import fr.overridescala.vps.ftp.api.task.{DynamicTaskCompleterFactory, TaskAchiever, TasksHandler}
import fr.overridescala.vps.ftp.api.utils.Utils
import fr.overridescala.vps.ftp.server.tasks.{AddressTaskCompleter, InitTaskCompleter}

import scala.collection.mutable

class ServerTaskCompleterFactory(private val tasksHandler: TasksHandler,
                                 private val server: RelayServer) extends DynamicTaskCompleterFactory {

    private lazy val completers: mutable.Map[String, DataPacket => TaskAchiever] = new mutable.HashMap[String, DataPacket => TaskAchiever]()

    override def getCompleter(channel: PacketChannel, initPacket: DataPacket): TaskAchiever = {
        val taskType = initPacket.header
        val content = initPacket.content
        taskType match {
            case "UP" => new DownloadTask(channel, tasksHandler, Utils.deserialize(content))
            case "DWN" => new UploadTask(channel, tasksHandler, Utils.deserialize(content))
            case "FINFO" => new FileInfoTask.Completer(channel, new String(content))
            case "ADR" => new AddressTaskCompleter(channel, server, new String(content))
            case "INIT" => new InitTaskCompleter(server, channel, new String(content))
            case _ => val completerSupplier = completers(taskType)
                if (completerSupplier == null)
                    throw new IllegalArgumentException("could not find completer for task " + taskType)
                completerSupplier.apply(initPacket)
        }
    }

    override def putCompleter(completerType: String, supplier: DataPacket => TaskAchiever): Unit =
        completers.put(completerType, supplier)
}
