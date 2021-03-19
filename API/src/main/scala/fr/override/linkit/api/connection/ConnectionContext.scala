package fr.`override`.linkit.api.connection

import fr.`override`.linkit.api.connection.network.{ConnectionState, Network}
import fr.`override`.linkit.api.connection.packet.traffic.{PacketInjectableContainer, PacketTraffic}
import fr.`override`.linkit.api.connection.task.TaskScheduler
import fr.`override`.linkit.api.local.concurrency.{IllegalThreadException, Procrastinator, workerExecution}
import fr.`override`.linkit.api.local.system.config.ConnectionConfiguration
import fr.`override`.linkit.api.local.system.event.EventNotifier

trait ConnectionContext extends PacketInjectableContainer with TaskScheduler with Procrastinator {
    val configuration: ConnectionConfiguration

    val identifier: String = configuration.identifier

    def traffic: PacketTraffic

    def network: Network

    val eventNotifier: EventNotifier

    def getState: ConnectionState

    @workerExecution
    @throws[IllegalThreadException]("If the current thread is not one of a BusyWorkerPool")
    def shutdown(): Unit
}
