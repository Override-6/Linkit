package fr.overridescala.vps.ftp.api.task

import java.io.IOException
import java.lang.reflect.InvocationTargetException

import fr.overridescala.vps.ftp.api.`extension`.packet.PacketManager
import fr.overridescala.vps.ftp.api.exceptions.{TaskException, TaskOperationFailException}
import fr.overridescala.vps.ftp.api.packet.SyncPacketChannel
import fr.overridescala.vps.ftp.api.packet.fundamental.TaskInitPacket
import fr.overridescala.vps.ftp.api.system.Reason

import scala.util.control.NonFatal

class TaskTicket(executor: TaskExecutor,
                 val channel: SyncPacketChannel,
                 val packetManager: PacketManager,
                 ownFreeWill: Boolean) {

    private val taskName: String = executor.getClass.getSimpleName
    private val notifier = packetManager.notifier

    def abort(reason: Reason): Unit = {
        notifyExecutor()
        executor match {
            case task: Task[_] =>
                val errorMethod = task.getClass.getMethod("error", classOf[String])
                errorMethod.setAccessible(true)
                notifier.onTaskSkipped(task, reason)
                try {
                    errorMethod.invoke(task, "Task aborted from an external handler")
                } catch {
                    case e: InvocationTargetException if e.getCause.isInstanceOf[TaskException] => Console.err.println(e.getMessage)
                    case e: InvocationTargetException => e.getCause.printStackTrace()
                    case e: TaskOperationFailException => Console.err.println(e.getMessage)
                    case NonFatal(e) => e.printStackTrace()
                } finally {
                    notifier.onTaskSkipped(task, reason)
                }
            case _ =>
        }
    }


    def start(): Unit = {
        var reason = Reason.INTERNAL_ERROR
        try {
            println(s"executing $taskName...")
            executor.init(packetManager, channel)

            executor match {
                case task: Task[_] => notifier.onTaskStartExecuting(task)
                case _ =>
            }

            if (ownFreeWill) {
                channel.sendPacket(TaskInitPacket.of(executor.initInfo)(channel))
            }

            executor.execute()
            reason = Reason.INTERNAL
            println(s"$taskName completed !")
        } catch {
            // Do not prints those exceptions : they are normal errors
            // lifted when a task is brutally aborted via Thread.interrupt
            case _: IllegalMonitorStateException =>
            case _: InterruptedException =>
            case e: IOException if e.getMessage != null && e.getMessage.equalsIgnoreCase("Broken pipe") =>

            case e: TaskOperationFailException => Console.err.println(e.getMessage)
            case NonFatal(e) => e.printStackTrace()
        } finally {
            notifyExecutor()
            executor match {
                case task: Task[_] => notifier.onTaskEnd(task, reason)
                case _ =>
            }
            executor.closeChannel(reason)
        }
    }

    private def notifyExecutor(): Unit = executor.synchronized {
        executor.notifyAll()
    }

}
