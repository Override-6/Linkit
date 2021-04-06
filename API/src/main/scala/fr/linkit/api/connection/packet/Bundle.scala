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

import fr.linkit.api.connection.packet.traffic.PacketChannel

/**
 * a Bundle is a class that contains a defined amount of typed items.
 * Bundle are useful to packet channels because they can be stored and processed later.
 *
 * @see [[PacketBundle]] for an implementation.
 * */
trait Bundle {

    val packet: Packet

    val attributes: PacketAttributes

    val coords: PacketCoordinates

    /**
     * The channel that have created this bundle.
     * */
    def getChannel: PacketChannel


    /**
     * Stores this bundle into the channel.
     * */
    def store(): Unit

    /**
     * Stores this bundle in the parent's of the bundle's channel only.
     * */
    def storeInParent(): Unit

    /**
     * Stores this bundle in the parent's of the bundle's channel and in its channel
     * */
    def storeWithParent(): Unit = {
        store()
        storeInParent()
    }

    /**
     * Stores this bundle into all parents of its channel's parent except for its channel
     * */
    def storeInAllParents(): Unit

    /**
     * Stores this bundle into all parents of its channel's parent and its channel.
     * */
    def storeWithAllParents(): Unit = {
        store()
        storeInAllParents()
    }

}
