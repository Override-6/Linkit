package fr.overridescala.vps.ftp.api

import fr.overridescala.vps.ftp.api.`extension`.RelayExtensionLoader
import fr.overridescala.vps.ftp.api.`extension`.event.EventDispatcher
import fr.overridescala.vps.ftp.api.exceptions.RelayInitialisationException
import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.`extension`.packet.PacketManager

//TODO reedit doc about all changes

/**
 * <p>
 * A Relay is the main interface implemented by the server (RelayServer) or client (RelayPoint).
 * Relay can be extended by using [[RelayExtensionLoader]], where extensions are jar files, loaded from "Extensions" Folder
 * Any Relay holds dynamic properties, that can be accessed, and modified from any extension.
 *
 * <p>
 * Each Relay have a unique identifier, if a client try to connect with the same identifier of another connected client
 * his connection will be refused. <br>
 * the RelayServer's identifier is forced to be "server". So, a client can't own this id
 * */
trait Relay extends RelayCloseable with TaskScheduler {

    /**
     * The unique identifier used to recognize a Relay in the network.<br>
     * Identifier is used by packets, but can be used for another context
     * */
    val identifier: String

    /**
     * @return the [[PacketManager]] used by this relay.<br>
     *         When the Relay receives or send a [[fr.overridescala.vps.ftp.api.packet.Packet]], it will automatically check into his packet manager to
     *         decompose the packet into byte seq, or build a [[fr.overridescala.vps.ftp.api.packet.Packet]] instance from byte seq.
     * @see [[PacketManager]]
     */
    val packetManager: PacketManager

    /**
     * @return the [[RelayExtensionLoader]] used by this relay.<br>
     *         any jar/extension dependence can be injected to this program by using the extensionLoader.
     * */
    val extensionLoader: RelayExtensionLoader

    /**
     * @return the [[RelayProperties]] used by this relay. <br>
     *         [[RelayProperties]] still empty while no extension decide to give him some properties.
     *         through [[RelayProperties]], object instances, parameters, etc can be share between extensions.
     * */
    val properties: RelayProperties

    /**
     * @return the [[EventDispatcher]] used by this relay.
     *         EventDispatcher is based on an Observer pattern, which carries all possible events that a relay can encounter
     *         excepted for packet events.
     * */
    val eventDispatcher: EventDispatcher

    /**
     * <b>Starts the Relay.</b>
     * <p>
     * This action is not automatically done, but is required to start the program
     * @throws RelayInitialisationException for any init error
     * */
    def start(): Unit

    /**
     * Builds [[PacketChannel.Sync]] instance. [[PacketChannel.ownerID]] will be this relay identifier
     *
     * @param linkedRelayID, the relay identifier where the packets will be send to and received
     * @param id, the channelID of the future packet channel.
     *
     * @see [[PacketChannel]]
     * @see [[PacketChannel.Sync]]
     * */
    def createSyncChannel(linkedRelayID: String, id: Int): PacketChannel.Sync

    /**
     * Builds [[PacketChannel.Async]] instance. [[PacketChannel.ownerID]] will be this relay identifier
     *
     * @param linkedRelayID, the relay identifier where the packets will be send to and received
     * @param id, the channelID of the future packet channel.
     *
     * @see [[PacketChannel]]
     * @see [[PacketChannel.Async]]
     * */
    def createAsyncChannel(linkedRelayID: String, id: Int): PacketChannel.Async

}
