package fr.`override`.linkit.api

import fr.`override`.linkit.api.concurrency.relayWorkerExecution
import fr.`override`.linkit.api.exception.{IllegalThreadException, RelayException}
import fr.`override`.linkit.api.extension.{RelayExtensionLoader, RelayProperties}
import fr.`override`.linkit.api.network.{ConnectionState, Network, RemoteConsole}
import fr.`override`.linkit.api.packet.Packet
import fr.`override`.linkit.api.packet.serialization.PacketTranslator
import fr.`override`.linkit.api.packet.traffic._
import fr.`override`.linkit.api.system._
import fr.`override`.linkit.api.system.config.RelayConfiguration
import fr.`override`.linkit.api.system.evente.EventNotifier
import fr.`override`.linkit.api.system.evente.extension.ExtensionEventHooks
import fr.`override`.linkit.api.system.evente.network.NetworkEventHooks
import fr.`override`.linkit.api.system.evente.packet.PacketEventHooks
import fr.`override`.linkit.api.system.evente.relay.RelayEventHooks
import fr.`override`.linkit.api.system.security.RelaySecurityManager
import fr.`override`.linkit.api.task.TaskScheduler
import org.apache.log4j.Logger
import org.jetbrains.annotations.Nullable

/**
 * The Relay trait is the core of this program.
 * Every features are accessible from a Relay, but some features needs the Relay to be started.
 * <p>
 * What is a Relay ? A Relay represents a program presence on the network.
 * With a relay, it is possible to schedule tasks between other relays, or simply create a [[PacketChannel]]
 * then start a packet conversation. Other internal functionalities are available such as event observation,
 * [[RelayExtensionLoader]] adds the possibility to create RelayExtensions
 *
 * @see [[RelayExtensionLoader]]
 * @see [[PacketTranslator]]
 * @see [[RelayProperties]]
 */
//TODO Recap :
//TODO Rewrite/write Doc and README of API, RelayServer and RelayPoint
//TODO Design a better event hooking system (Object EventCategories with sub parts like ConnectionListeners, PacketListeners, TaskListeners...)
//TODO Find a solution about packets that are send into a non-registered channel : if an exception is thrown, this can cause some problems, and if not, this can cause other problems. SOLUTION : Looking for "RemoteActionDescription" that can control and get some information about an action that where made over the network.
//TODO Create a PacketTree that can let the RelaySecurityManager know the content and the structure of a packet without casting it or making weird reflection stuff.
//TODO Replace all Any types by Serializable types in network.cache
object Relay {
    val ApiVersion: Version = Version(name = "Api", version = "0.20.0", stable = false)
    val ServerIdentifier: String = "server"
    val Log: Logger = Logger.getLogger("Relay", new RelayLogger(_))
}

trait Relay extends JustifiedCloseable with TaskScheduler with PacketInjectableContainer {

    /**
     * Configuration set of this relay.
     * The value is the instance of the configuration that is used
     * by the relay. Configuration let the user define some options
     *
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
     * The current versioning system is the SemVer, but, during the beta phase of this project,
     * it will not being respected.
     * This way, the first number is always 0, any feature or irreversible change would increment the middle number,
     * and small reversible patches increments the last number.
     *
     * During beta phase :
     * 0.x.y
     *
     * x feature, irreversible internal changes or remote changes such as packet protocol or communication
     * y patches, internal bugfixes, that are fully reversible
     * No objectives are scheduled in order to exit the beta phase.
     */
    val relayVersion: Version

    /**
     * SecurityManager is an interface that handles the security about the relays.
     * In order to inject a [[RelaySecurityManager]] instance, you have to set the 'securityManager' option of the [[RelayConfiguration]]
     * that will be used by a relay.
     * Be aware that the relay implementation can require a more specific [[RelaySecurityManager]].
     * */
    val securityManager: RelaySecurityManager

    /**
     * The Packet translator used by this relay.
     * This translator is responsible of serializing and deserializing packets.
     * The serialization method can vary according to some constraint.
     *
     * @see [[PacketTranslator]] for further details.
     * */
    val packetTranslator: PacketTranslator

    /**
     * The Extension Loader of this relay.
     * Contains every loaded RelayExtension and ExtensionFragments used by the relay.
     *
     * @see [[RelayExtensionLoader]]
     */
    val extensionLoader: RelayExtensionLoader

    /**
     * The Relay properties defines runtime values. it must not be used for storage
     * [[RelayProperties]] can be modified by other relays using [[fr.`override`.linkit.api.network.NetworkEntity]]
     */
    val properties: RelayProperties

    /**
     * The [[PacketTraffic]] handles all [[Packet]] objects that must be sent or injected
     * only one PacketTraffic is used by the relay.
     * [[PacketTraffic]] can also be used to get a [[PacketWriter]], the packet writer will
     * then directly send all provided packet to the targeted relay using [[fr.`override`.linkit.api.packet.PacketCoordinates]]
     * Using a [[PacketWriter]] is ok to send packets, but packet channels have been created in order to
     * handle packet sending and packet receiption easily and are therefore more adapted to send packets.
     *
     * @see [[PacketInjectable]] for further packet injection details.
     * @see [[PacketTranslator]] & [[PacketWriter]] for further details about packet upload.
     * */
    val traffic: PacketTraffic

    //////////////////////// Event system ////////////////////////////

    /**
     * [[EventNotifier]] is used to trigger an event that concern this relay.
     * the package [[fr.`override`.linkit.api.system.evente]] is designed to
     * use the busy threading system, and be extensible
     */
    val eventNotifier: EventNotifier

    /*
     * The 4 fields are a set of event that are categorised into EventHookCategory classes
     * They are used by the user to hook events and register listeners to the notifier.
     * The notifier also uses hooks in order to determine where the triggered event should be landed
     * */

    /**
     * Represents a set of events the relay can trigger, like being ready or disconnected.
     * */
    implicit val relayHooks: RelayEventHooks = new RelayEventHooks

    /**
     * Represents a set of events the relay can trigger, like being ready or disconnected.
     * */
    implicit val extensionHooks: ExtensionEventHooks = new ExtensionEventHooks

    /**
     * Represents a set of events the packet system of this relay can trigger, such as sending or injecting a packet.
     * */
    implicit val packetHooks: PacketEventHooks = new PacketEventHooks

    /**
     * Represents a set of network events that can be triggered. such as a new entity that has been
     * connected.
     */
    implicit val networkHooks: NetworkEventHooks = new NetworkEventHooks


    ///////////////////////// Start and stop //////////////////////////

    /**
     * Will Start the Relay in the current thread.
     * This method will load every local and remote feature,
     * enable everything that needs to be enabled, and perform some security checks before logging to the server.
     *
     * @throws RelayException         if something went wrong, In Local, during the fr.override.linkit.client-to-server, or fr.override.linkit.client-to-network initialisation.
     * @throws IllegalThreadException if this method is not executed in one of the [[RelayThreadPool]] threads.
     * */
    @relayWorkerExecution
    @throws[IllegalThreadException]("If the current thread is not one of a RelayThreadPool")
    @throws[RelayException]("If the startup went wrong")
    def start(): Unit

    /**
     * Closes this relay, disconnects from the network then shutdowns the thread pools
     */
    @relayWorkerExecution
    @throws[IllegalThreadException]("If the current thread is not one of a RelayThreadPool")
    override def close(): Unit = super.close()

    /**
     * Closes this relay, disconnects from the network then shutdowns the thread pools
     *
     * @param reason gives a reason for the close request.
     *               the close behaviour can vary if the reason is either internal or external
     * */
    @relayWorkerExecution
    @throws[IllegalThreadException]("If the current thread is not one of a RelayThreadPool")
    override def close(reason: CloseReason): Unit

    //////////////////////////// NETWORK ACCESSORS ///////////////////////////////////

    /**
     * Accessor for the Network instance that contains all the [[fr.`override`.linkit.api.network.NetworkEntity]]
     * connected on the network.
     *
     * @return the network accessor object
     * */
    def network: Network

    /**
     * The current state of the relay, the default state is set to [[RelayState.INACTIVE]]
     *
     * @return the state enum
     * @see [[RelayState]] for further details
     * */
    def state(): RelayState

    /**
     * Will run this callback in a worker thread,
     * owned by the thread pool of this relay.
     *
     * @see [[fr.`override`.linkit.api.concurrency.RelayThreadPool]]
     * */
    def runLater(callback: => Unit): this.type

    /**
     * @return the connection state of this relay
     * @see [[ConnectionState]]
     * */
    def getConnectionState: ConnectionState

    /**
     * @param identifier the relay identifier to check
     * @return true if the given relay identifier is connected on the network
     * */
    def isConnected(identifier: String): Boolean

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
