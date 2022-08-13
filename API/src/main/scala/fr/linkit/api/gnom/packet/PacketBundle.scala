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
