package fr.`override`.linkit.api.task

import java.io.IOException
import java.lang.reflect.InvocationTargetException

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.exception.{TaskException, TaskOperationFailException}
import fr.`override`.linkit.api.packet.fundamental.TaskInitPacket
import fr.`override`.linkit.api.packet.traffic.ChannelScope
import fr.`override`.linkit.api.packet.traffic.channel.SyncPacketChannel
import fr.`override`.linkit.api.system.CloseReason

import scala.util.control.NonFatal

class TaskTicket(executor: TaskExecutor,
                 relay: Relay,
                 taskId: Int,
                 target: String,
                 ownFreeWill: Boolean) {

    private val errRemote = relay.getConsoleErr(target)
    val channel: SyncPacketChannel = relay.createInjectable(taskId, ChannelScope.immutable(target), SyncPacketChannel)

    def abort(reason: CloseReason): Unit = {
        notifyExecutor()
        executor match {
            case task: Task[_] =>
                val errorMethod = task.getClass.getMethod("fail", classOf[String])
                errorMethod.setAccessible(true)

                try {
                    errorMethod.invoke(task, "Task aborted from an external handler")
                } catch {
                    case e: InvocationTargetException if e.getCause.isInstanceOf[TaskException] => Console.err.println(e.getMessage)
                    case e: InvocationTargetException => e.getCause.printStackTrace()
                    case e: TaskOperationFailException =>
                        Console.err.println(e.getMessage)
                        e.printStackTrace(errRemote)

                    case NonFatal(e) => e.printStackTrace()
                        e.printStackTrace(errRemote)
                }
            case _ =>
        }
    }


    def start(): Unit = {
        var reason = CloseReason.INTERNAL_ERROR
        try {
            executor.init(relay, channel)

            if (ownFreeWill) {
                val initInfo = executor.initInfo
                channel.send(TaskInitPacket(initInfo.taskType, initInfo.content))
            }

            executor.execute()
            reason = CloseReason.INTERNAL
        } catch {
            // Do not prints those exceptions : they are normal errors
            // lifted when a task execution is brutally aborted
            case _: IllegalMonitorStateException =>
            case _: InterruptedException =>
            case e: IOException if e.getMessage != null && e.getMessage.equalsIgnoreCase("Broken pipe") =>

            case e: TaskOperationFailException =>
                Console.err.println(e.getMessage)
                e.printStackTrace(errRemote)

            case NonFatal(e) =>
                e.printStackTrace()
                e.printStackTrace(errRemote)

        } finally {
            notifyExecutor()
            executor.closeChannel(reason)
        }
    }

    private def notifyExecutor(): Unit = executor.synchronized {
        executor.notifyAll()
    }

}
