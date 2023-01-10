/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.persistence

import fr.linkit.api.gnom.packet.{Packet, PacketAttributes, PacketCoordinates}

import java.nio.ByteBuffer

trait PacketTransfer {


    def coords: PacketCoordinates

    def attributes: PacketAttributes

    def packet: Packet

}
