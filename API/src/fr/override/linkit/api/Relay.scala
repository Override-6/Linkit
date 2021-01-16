package fr.`override`.linkit.api

import fr.`override`.linkit.api.`extension`.{RelayExtensionLoader, RelayProperties}
import fr.`override`.linkit.api.exception.{IllegalPacketWorkerLockException, RelayInitialisationException}
import fr.`override`.linkit.api.network.{ConnectionState, Network, RemoteConsole}
import fr.`override`.linkit.api.packet.channel.{PacketChannel, PacketChannelFactory}
import fr.`override`.linkit.api.packet.collector.{PacketCollector, PacketCollectorFactory}
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.packet.{Packet, PacketFactory, PacketTranslator}
import fr.`override`.linkit.api.system.config.RelayConfiguration
import fr.`override`.linkit.api.system.security.RelaySecurityManager
import fr.`override`.linkit.api.system.{JustifiedCloseable, Version}
import fr.`override`.linkit.api.task.TaskScheduler
import org.jetbrains.annotations.Nullable

import scala.reflect.ClassTag

/**
 * The Relay trait is the core of this program.
 * Every features are accessible from a Relay, but some features needs the Relay to be started.
 * <p>
 * What is a Relay ? A Relay represents a program presence on the network.
 * With a relay, it is possible to schedule tasks between other relays, or simply create a [[PacketChannel]]
 * then start a packet conversation. Other local functionalities are available such as event observation,
 * [[RelayExtensionLoader]] adds the possibility to create RelayExtensions
 *
 * @see [[RelayExtensionLoader]]
 * @see [[PacketTranslator]]
 * @see [[RelayProperties]]
 */
//TODO Recap :
//TODO Rewrite/write Doc and README of API, RelayServer and RelayPoint
//TODO Design a better event hooking system (Object EventCategories with sub parts like ConnectionListeners, PacketListeners, TaskListeners...)
//TODO Replace every "OK" and "ERROR" by 0 or 1
//TODO Design a brand new and optimised packet protocol
//TODO Find a solution about packets that are send into a non-registered channel : if an exception is thrown, this can cause some problems, and if not, this can cause other problems. SOLUTION : Looking for "RemoteActionDescription" that can control and get some information about an action that where made over the network.
//TODO Think about a more useful, powerful and stable api.util.cache system
object Relay {
    val ApiVersion: Version = Version(name = "Api", version = "0.17.0", stable = false)
    val ServerIdentifier: String = "server"
}

trait Relay extends JustifiedCloseable with TaskScheduler {
    /**
     * The currently used Configuration of this relay.
     * @see [[RelayConfiguration]]
     * */
    val configuration: RelayConfiguration

    /**
     * A Relay identifier is a string that identifies the Relay on the network.
     * No IP address is intended.
     * The identifier have to match \w{0,16} to be used threw the network
     */
    val identifier: String = configuration.identifier

    /**
     * The implementation version represented by following the SemVer convention.
     */
    val relayVersion: Version

    val securityManager: RelaySecurityManager

    /**
     * The Packet Manager used by this relay.
     * A packetTranslator can register a [[Packet]]
     * then build or decompose a packet using [[PacketFactory]]
     *
     * @see packetTranslator on how to register and use a customised packet kind
     * */
    val packetTranslator: PacketTranslator

    /**
     * The Extension Loader of this relay.
     * Contains every loaded RelayExtension used by the relay.
     */
    val extensionLoader: RelayExtensionLoader

    /**
     * The Relay properties must be used/updated by the extensions, and is Therefore empty by default.
     */
    val properties: RelayProperties

    /**
     * Packet Worker Threads have to be registered in this ThreadGroup in order to throw an exception when a relay worker thread
     * is about to be locked by a monitor, that concern packet reception (example: lockers of BlockingQueues in PacketChannels)
     *
     * @see [[IllegalPacketWorkerLockException]]
     * */
    val packetWorkerThreadGroup: ThreadGroup = new ThreadGroup("Relay Packet Workers")

    /**
     * The network object of this relay, this object is such a [[fr.`override`.linkit.api.network.NetworkEntity]] container
     * with some getters. No network interaction can be done through object.
     * */
    val network: Network

    val traffic: PacketTraffic

    /**
     * Starts the Relay by loading every features.
     *
     * @throws RelayInitialisationException if the relay could not start properly
     */
    def start(): Unit

    /**
     * @param identifier the relay identifier to check
     * @return true if the given relay identifier is connected on the network
     * */
    def isConnected(identifier: String): Boolean

    /**
     * Adds a function that which be called when the relay's connection is updated
     * */
    def addConnectionListener(action: ConnectionState => Unit)

    /**
     * @return the connection state of this relay
     * @see [[ConnectionState]]
     * */
    def getState: ConnectionState

    def openChannel[C <: PacketChannel : ClassTag](channelId: Int, targetID: String, factory: PacketChannelFactory[C]): C

    def openCollector[C <: PacketCollector : ClassTag](channelId: Int, factory: PacketCollectorFactory[C]): C

    /**
     * @param targetId the targeted Relay identifier
     * @return the Console out controller of the specified relay
     */
    def getConsoleOut(@Nullable targetId: String): RemoteConsole

    /**
     * @param targetId the targeted Relay identifier
     * @return the Console err controller of the specified relay
     */
    def getConsoleErr(@Nullable targetId: String): RemoteConsole


}
