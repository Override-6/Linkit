package fr.overridescala.vps.ftp.server.task

import java.io.IOException
import java.lang.reflect.InvocationTargetException

import fr.overridescala.vps.ftp.api.Reason
import fr.overridescala.vps.ftp.api.exceptions.{TaskException, TaskOperationException}
import fr.overridescala.vps.ftp.api.packet.SyncPacketChannel
import fr.overridescala.vps.ftp.api.task.{Task, TaskExecutor}
import fr.overridescala.vps.ftp.server.RelayServer

import scala.util.control.NonFatal

class TaskTicket(executor: TaskExecutor,
                 taskID: Int,
                 targetID: String,
                 server: RelayServer,
                 ownFreeWill: Boolean) {

    var channel: SyncPacketChannel = server.createSync(targetID, taskID)
    private val taskName: String = executor.getClass.getSimpleName
    private val notifier = server.eventDispatcher.notifier

    def abort(reason: Reason): Unit = {
        notifyExecutor()
        executor match {
            case task: Task[_] =>
                val errorMethod = task.getClass.getMethod("error", classOf[String])
                errorMethod.setAccessible(true)
                server.eventDispatcher.notifier.onTaskSkipped(task, reason)
                try {
                    errorMethod.invoke(task, "Task aborted from an external handler")
                } catch {
                    case e: InvocationTargetException if e.getCause.isInstanceOf[TaskException] => Console.err.println(e.getMessage)
                    case e: InvocationTargetException => e.getCause.printStackTrace()
                    case e: TaskOperationException => Console.err.println(e.getMessage)
                    case NonFatal(e) => e.printStackTrace()
                } finally {
                    notifier.onTaskSkipped(task, reason)
                }
            case _ =>
        }
    }


    def start(): Unit = {
        var reason = Reason.ERROR_OCCURRED
        try {
            println(s"executing $taskName...")
            executor.init(server.packetManager, channel)

            executor match {
                case task: Task[_] => notifier.onTaskStartExecuting(task)
                case _ =>
            }

            if (ownFreeWill) {
                channel.sendInitPacket(executor.initInfo)
            }
            executor.execute()
            reason = Reason.LOCAL_REQUEST
            println(s"$taskName completed !")
        } catch {
            // Do not prints those exceptions : they are normal errors
            // lifted when a task is brutally aborted via Thread.interrupt
            case _: IllegalMonitorStateException =>
            case _: InterruptedException =>
            case e: IOException if e.getMessage != null && e.getMessage.equalsIgnoreCase("Broken pipe") =>

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
