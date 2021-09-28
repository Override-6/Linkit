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

package fr.linkit.api.application.packet

trait PacketAttributes extends Serializable {

    def getAttribute[S](key: Serializable): Option[S]

    //def getPresence[S](presence: PacketAttributesPresence): Option[S]

    def putAttribute(key: Serializable, value: Serializable): this.type

    //def putPresence(presence: PacketAttributesPresence, value: Serializable): this.type

    def drainAttributes(other: PacketAttributes): this.type

    def foreachAttributes(f: (Serializable, Serializable) => Unit): this.type

    def putAllAttributes(other: PacketAttributes): this.type = {
        other.drainAttributes(this)
        this
    }

    def putAllAttributes(presence: PacketAttributesPresence): this.type = {
        presence.drainAllAttributes(this)
        this
    }

    def isEmpty: Boolean

    def nonEmpty: Boolean = !isEmpty

}
