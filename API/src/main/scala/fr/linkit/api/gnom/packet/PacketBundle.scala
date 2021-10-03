/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.api.gnom.packet

/**
 * a Bundle is a class that contains a defined amount of typed items.
 * Bundle are useful to packet channels because they can be stored and processed later.
 *
 * @see [[PacketBundle]] for an implementation.
 * */
trait PacketBundle {

    val packet: Packet

    val attributes: PacketAttributes

    val coords: PacketCoordinates


}
