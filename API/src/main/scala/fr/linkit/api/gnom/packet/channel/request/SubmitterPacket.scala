/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.packet.channel.request

import fr.linkit.api.gnom.packet.{Packet, PacketAttributes}

import scala.reflect.ClassTag

trait SubmitterPacket extends Packet {

    @throws[NoSuchElementException]("If this method is called more times than packet array's length" + this)
    def nextPacket[P <: Packet : ClassTag]: P

    def nextPacket[P <: Packet : ClassTag](packet: P => Unit): this.type = {
        packet(nextPacket[P])
        this
    }

    def nextPacket: Packet = nextPacket[Packet]

    def foreach(action: Packet => Unit): this.type

    def getAttributes: PacketAttributes

    def getAttribute[S](key: Serializable): Option[S]

    def putAttribute(key: Serializable, value: Serializable): this.type
}
