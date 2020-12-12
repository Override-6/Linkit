package fr.overridescala.linkkit.api

import fr.overridescala.linkkit.api.`extension`.{RelayExtensionLoader, RelayProperties}
import fr.overridescala.linkkit.api.exception.RelayInitialisationException
import fr.overridescala.linkkit.api.packet.{PacketManager, TrafficHandler}
import fr.overridescala.linkkit.api.packet.channel.PacketChannel
import fr.overridescala.linkkit.api.packet.collector.PacketCollector
import fr.overridescala.linkkit.api.system.config.RelayConfiguration
import fr.overridescala.linkkit.api.system.event.EventObserver
import fr.overridescala.linkkit.api.system.security.RelaySecurityManager
import fr.overridescala.linkkit.api.system.{JustifiedCloseable, RemoteConsole, Version}
import fr.overridescala.linkkit.api.task.TaskScheduler
import org.jetbrains.annotations.Nullable

/**
 * The Relay trait is the core of this program.
 * Every features are accessible from a Relay, but some features needs the Relay to be started.
 * <p>
 * What is a Relay ? A Relay represents a program presence on the network.
 * With a relay, it is possible to schedule tasks between other relays, or simply create a [[PacketChannel]]
 * to start a packet conversation. Other local functionalities are available such as event observation,
 * [[RelayExtensionLoader]] adds the possibility to create RelayExtensions
 *
 * @see [[RelayExtensionLoader]]
 * @see [[PacketManager]]
 * @see [[RelayProperties]]
 * @see [[EventObserver]]
 */

object Relay {
    final val apiVersion = Version("Api", "0.7.0", stable = false)
}

trait Relay extends JustifiedCloseable with TaskScheduler {

    /**
     * The API Version represented by following the SemVer convention.
     */
    final val apiVersion = Relay.apiVersion

    /**
     * The implementation version represented by following the SemVer convention.
     */
    val relayVersion: Version

    val configuration: RelayConfiguration

    val securityManager: RelaySecurityManager

    val trafficHandler: TrafficHandler

    /**
     * A Relay identifier is a string that identifies the Relay on the network.
     * No IP address is intended.
     * The identifier have to match \w{0,16} to be used threw the network
     */
    val identifier: String = configuration.identifier

    /**
     * The Packet Manager used by this relay.
     * A PacketManager can register a [[fr.overridescala.linkkit.api.packet.Packet]]
     * then build or decompose a packet using [[fr.overridescala.linkkit.api.`extension`.packet.PacketFactory]]
     *
     * @see PacketManager on how to register and use a customised packet kind
     * */
    val packetManager: PacketManager

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
     * @see [[fr.overridescala.linkkit.api.exception.IllegalPacketWorkerLockException]]
     * */
    val packetWorkerThreadGroup: ThreadGroup = new ThreadGroup("Relay Packet Workers")

    /**
     * The Event Observer used by this relay.
     */
    @deprecated
    val eventObserver: EventObserver

    /**
     * Starts the Relay by loading every features.
     *
     * @throws RelayInitialisationException if the relay could not start properly
     */
    def start(): Unit

    /**
     * @param linkedRelayID the targeted relay identifier to connect
     * @param id            the PacketChannel identifier
     * @return a synchronous packet channel linked with the specified relay
     *
     * @see [[PacketChannel]]
     */
    def createSyncChannel(linkedRelayID: String, id: Int, cacheSize: Int = configuration.defaultContainerPacketCacheSize): PacketChannel.Sync

    /**
     * @param linkedRelayID the targeted relay identifier to connect
     * @param id            the PacketChannel identifier
     * @return an asynchronous packet channel linked with the specified relay
     *
     * @see [[PacketChannel]]
     */
    def createAsyncChannel(linkedRelayID: String, id: Int): PacketChannel.Async

    /**
     * @param id the identifier to attribute with the [[PacketCollector]]
     * @return a synchronised [[PacketCollector]]
     *
     * @see [[PacketCollector]], [[PacketCollector.Sync]]
     * */
    def createSyncCollector(id: Int, cacheSize: Int = configuration.defaultContainerPacketCacheSize): PacketCollector.Sync

    /**
     * @param id the identifier to attribute with the [[PacketCollector]]
     * @return an asynchronous [[PacketCollector]]
     *
     * @see [[PacketCollector]], [[PacketCollector.Async]]
     * */
    def createAsyncCollector(id: Int): PacketCollector.Async

    /**
     * @param targetId the targeted Relay identifier
     * @return the Console out controller of the specified relay
     */
    def getConsoleOut(@Nullable targetId: String): Option[RemoteConsole]

    /**
     * @param targetId the targeted Relay identifier
     * @return the Console err controller of the specified relay
     */
    def getConsoleErr(@Nullable targetId: String): Option[RemoteConsole.Err]


}
