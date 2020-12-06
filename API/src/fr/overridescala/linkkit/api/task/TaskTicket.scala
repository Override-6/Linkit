package fr.overridescala.linkkit.api.task

import java.io.IOException
import java.lang.reflect.InvocationTargetException

import fr.overridescala.linkkit.api.Relay
import fr.overridescala.linkkit.api.exceptions.{TaskException, TaskOperationFailException}
import fr.overridescala.linkkit.api.packet.channel.PacketChannel
import fr.overridescala.linkkit.api.packet.fundamental.TaskInitPacket
import fr.overridescala.linkkit.api.system.Reason

import scala.util.control.NonFatal

class TaskTicket(executor: TaskExecutor,
                 relay: Relay,
                 taskId: Int,
                 target: String,
                 ownFreeWill: Boolean) {

    private val notifier = relay.eventObserver.notifier
    private val errConsole = relay.getConsoleErr(target).get
    val channel: PacketChannel.Sync = relay.createSyncChannel(target, taskId)


    def abort(reason: Reason): Unit = {
        notifyExecutor()
        executor match {
            case task: Task[_] =>

                val errorMethod = task.getClass.getMethod("fail", classOf[String])
                errorMethod.setAccessible(true)
                notifier.onTaskSkipped(task, reason)

                try {
                    errorMethod.invoke(task, "Task aborted from an external handler")
                } catch {
                    case e: InvocationTargetException if e.getCause.isInstanceOf[TaskException] => Console.err.println(e.getMessage)
                    case e: InvocationTargetException => e.getCause.printStackTrace()
                    case e: TaskOperationFailException =>
                        Console.err.println(e.getMessage)
                        errConsole.reportException(e)

                    case NonFatal(e) => e.printStackTrace()
                        errConsole.reportException(e)
                } finally {
                    notifier.onTaskSkipped(task, reason)
                }
            case _ =>
        }
    }


    def start(): Unit = {
        var reason = Reason.INTERNAL_ERROR
        try {
            executor.init(relay, target, channel)

            executor match {
                case task: Task[_] => notifier.onTaskStartExecuting(task)
                case _ =>
            }

            if (ownFreeWill) {
                val initInfo = executor.initInfo
                channel.sendPacket(TaskInitPacket(initInfo.taskType, initInfo.content))
            }

            executor.execute()
            reason = Reason.INTERNAL
        } catch {
            // Do not prints those exceptions : they are normal errors
            // lifted when a task execution is brutally aborted
            case _: IllegalMonitorStateException =>
            case _: InterruptedException =>
            case e: IOException if e.getMessage != null && e.getMessage.equalsIgnoreCase("Broken pipe") =>

            case e: TaskOperationFailException =>
                Console.err.println(e.getMessage)
                errConsole.reportException(e)

            case NonFatal(e) =>
                e.printStackTrace()
                errConsole.reportException(e)

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
