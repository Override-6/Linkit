/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.api.connection.packet

import fr.linkit.api.connection.packet.Packet.nextPacketID

//TODO Doc
trait Packet extends Serializable {

    @transient private var id = nextPacketID

    def className: String = getClass.getSimpleName

    def number: Int = id

    /**
     * If the packet has been instantiated using Unsafe.allocateInstance
     * The constructor will not be called thus its number identifier is not attributed.
     * This method will manually give this packet an identifier, but calling this method
     * takes effect only once.
     *
     * The number is used by traffic classes and more specifically during Injection.
     * */
    def prepare(): this.type = {
        if (id <= 0) {
            id = nextPacketID
        }
        this
    }

}

object Packet {
    @volatile private var packetID = 0

    private def nextPacketID: Int = {
        packetID += 1
        packetID
    }
}