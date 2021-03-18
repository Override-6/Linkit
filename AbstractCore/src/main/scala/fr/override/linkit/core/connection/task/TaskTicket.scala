package fr.`override`.linkit.core.connection.task

import java.io.IOException
import java.lang.reflect.InvocationTargetException

import fr.`override`.linkit.core.connection.packet
import fr.`override`.linkit.core.connection.packet.traffic
import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.connection.packet.fundamental.TaskInitPacket
import fr.`override`.linkit.api.connection.packet.traffic.ChannelScope
import fr.`override`.linkit.api.connection.packet.traffic.channel.SyncPacketChannel
import fr.`override`.linkit.api.local.system.CloseReason

import scala.util.control.NonFatal

class TaskTicket(executor: SimpleTaskExecutor,
                 relay: Relay,
                 taskId: Int,
                 target: String,
                 ownFreeWill: Boolean) {

    private val errRemote = relay.getConsoleErr(target)
    val channel: packet.traffic.channel.SyncPacketChannel = relay.getInjectable(taskId, ChannelScope.reserved(target), traffic.channel.SyncPacketChannel)

    def abort(reason: CloseReason): Unit = {
        notifyExecutor()
        executor match {
            case task: SimpleTask[_] =>
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
