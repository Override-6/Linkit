package fr.`override`.linkit.client

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.connection.network.{ConnectionState, Network}
import fr.`override`.linkit.api.connection.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.connection.task.Task
import fr.`override`.linkit.api.local.system.config.ConnectionConfiguration
import fr.`override`.linkit.api.local.system.event.EventNotifier
import fr.`override`.linkit.client.network.PointNetwork
import fr.`override`.linkit.core.connection.packet.traffic.DynamicSocket

class ClientConnection(socket: DynamicSocket, override val configuration: ConnectionConfiguration) extends ConnectionContext {

    override def traffic: PacketTraffic = ???

    override def network: Network = new PointNetwork()

    override val eventNotifier: EventNotifier = _

    override def getState: ConnectionState = ???

    override def shutdown(): Unit = ???

    override def runLater(task: => Unit): Unit = ???

    /**
     * @return the [[TaskCompleterHandler]] used by this Relay.
     * @see [[TaskCompleterHandler]]
     * */
    override val taskCompleterHandler: Any = _

    /**
     * schedules a TaskExecutor.
     *
     * @param task the task to schedule
     * @return a [[RelayTaskAction]] instance, this object allows you to enqueue or complete the task later.
     * @see [[RelayTaskAction]]
     * */
    override def scheduleTask[R](task: Task[R]): RelayTaskAction[R] = ???
}
