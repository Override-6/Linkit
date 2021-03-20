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

package fr.`override`.linkit.api.packet

import fr.`override`.linkit.api.packet.serialization.NumberSerializer

import java.util

object PacketUtils {

    def stringUntilEnd(a: Array[Byte])(implicit src: Array[Byte]): String =
        new String(untilEnd(a))

    def untilEnd(a: Array[Byte])(implicit src: Array[Byte]): Array[Byte] =
        util.Arrays.copyOfRange(src, src.indexOfSlice(a) + a.length, src.length)

    def stringBetween(a: Array[Byte], b: Array[Byte])(implicit src: Array[Byte]) =
        new String(between(a, b))

    def between(a: Array[Byte], b: Array[Byte])(implicit src: Array[Byte]): Array[Byte] =
        util.Arrays.copyOfRange(src, src.indexOfSlice(a) + a.length, src.indexOfSlice(b))

    def wrap(bytes: Array[Byte]): Array[Byte] = {
        NumberSerializer.serializeInt(bytes.length) ++ bytes
    }

}
