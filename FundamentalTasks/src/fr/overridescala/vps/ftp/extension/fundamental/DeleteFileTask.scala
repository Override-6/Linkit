package fr.overridescala.vps.ftp.`extension`.fundamental

import java.io.{FileNotFoundException, IOException}
import java.nio.file.{Files, Path, Paths}

import fr.overridescala.vps.ftp.api.packet.fundamental.{EmptyPacket, ErrorPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor, TaskInitInfo}
import DeleteFileTask.TYPE

case class DeleteFileTask(targetId: String, targetPath: String) extends Task[Unit](targetId) {

    override val initInfo: TaskInitInfo =
        TaskInitInfo.of(TYPE, targetID, targetPath)

    override def execute(): Unit = {
        channel.nextPacket() match {
            case errorPacket: ErrorPacket =>
                errorPacket.printError()
                fail(errorPacket.errorMsg)
            case _: EmptyPacket => success()
        }
    }
}

object DeleteFileTask {
    val TYPE = "DLF"

    class Completer(pathString: String) extends TaskExecutor {

        override def execute(): Unit = {
            val path = Paths.get(pathString)
            if (Files.notExists(path)) {
                channel.sendPacket(ErrorPacket(
                    classOf[FileNotFoundException].getName,
                    s"could not find file ot folder in path $path",
                    s"$path not exists"))
                return
            }
            try {
                Files.delete(path)
            } catch {
                case e: IOException =>
                    channel.sendPacket(ErrorPacket(
                        e.getClass.getName,
                        s"could not delete file or folder in $path",
                        e.getMessage
                    ))
                    Console.err.println(e.getMessage)
            }
            channel.sendPacket(EmptyPacket())
        }

    }

}

