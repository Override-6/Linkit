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

//TODO --------------------------------------------- PARTIALLY MAINTAINED ---------------------------------------------

trait PacketAttributesPresence {

    def addDefaultAttribute(key: Serializable, value: Serializable): this.type

    //def addDefaultPresence(presence: PacketAttributesPresence, value: Serializable): this.type

    def getDefaultAttribute[S](key: Serializable): Option[S]

    //def getDefaultPresence[S](presence: PacketAttributesPresence): Option[S]

    def drainAllAttributes(packetAttributes: PacketAttributes): this.type

    def drainAllDefaultAttributes(packetAttributes: PacketAttributesPresence): this.type

}
