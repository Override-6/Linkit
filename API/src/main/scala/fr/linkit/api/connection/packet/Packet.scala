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

package fr.linkit.api.connection.packet

import fr.linkit.api.connection.packet.Packet.nextPacketID
import fr.linkit.api.connection.packet.traffic.injection.InjectionHelper

//TODO Doc
trait Packet extends Serializable {

    @volatile
    @transient private var id = nextPacketID

    @volatile
    @transient private var helper: InjectionHelper = InjectionHelper(id)

    def className: String = getClass.getSimpleName

    /**
     * If the packet has been instantiated using Unsafe.allocateInstance
     * The constructor will not be called thus his helper is not initialized.
     * This method will manually give this packet a helper, but calling this method
     * takes effect only once.
     *
     * The helper is used by traffic classes and more specifically for Injection.
     * It contains the packet number and determines if this packet could be injected multiple times.
     * */
    def getHelper: InjectionHelper = {
        if (helper == null) {
            id = nextPacketID
            helper = InjectionHelper(id)
        }
        helper
    }

}

object Packet {
    @volatile private var packetID = 0

    private def nextPacketID: Int = {
        packetID += 1
        packetID
    }
}