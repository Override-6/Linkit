/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.core.connection.network

//TODO -------------------------------------------------- MAINTAINED --------------------------------------------------
class RemoteConsolesContainer() {

   /* private val printChannel = relay.getInjectable(PacketTraffic.RemoteConsoles, ChannelScope.broadcast, channel.AsyncPacketChannel)

    private val outConsoles = Collections.synchronizedMap(new ConcurrentHashMap[String, SimpleRemoteConsole])
    private val errConsoles = Collections.synchronizedMap(new ConcurrentHashMap[String, SimpleRemoteConsole])

    def getOut(targetId: String): SimpleRemoteConsole = get(targetId, SimpleRemoteConsole.out, outConsoles)

    def getErr(targetId: String): SimpleRemoteConsole = get(targetId, SimpleRemoteConsole.err, errConsoles)

    private def get(targetId: String, supplier: (packet.traffic.channel.AsyncPacketChannel, Relay, String) => SimpleRemoteConsole, consoles: util.Map[String, SimpleRemoteConsole]): SimpleRemoteConsole = {
        if (!relay.configuration.enableRemoteConsoles)
            return SimpleRemoteConsole.Mock

        if (targetId == relay.identifier)
            throw new AppException("Attempted to get remote console of this current relay")

        if (consoles.containsKey(targetId))
            return consoles.get(targetId)

        val channel = printChannel.subInjectable(targetId, traffic.channel.AsyncPacketChannel, true)
        val console = supplier(channel, relay, targetId)
        consoles.put(targetId, console)

        console
    }

    init()

    protected def init(): Unit = {
        printChannel.addOnPacketReceived((packet, coords) => {
            packet match {
                case TaggedObjectPacket(header, msg: String) =>
                    val log: String => Unit = if (header == "err") Log.error else Log.info
                    log(s"[${coords.senderID}]: $msg")
                    val entity = relay.network.getEntity(coords.senderID).get
                    val event = NetworkEvents.remotePrintReceivedEvent(entity, msg)
                    relay.eventNotifier.notifyEvent(event)
                case other: Packet => throw new UnexpectedPacketException(s"Unexpected packet '${other.getClass.getName}' injected in a remote console.")
            }
        })
    }*/

}
