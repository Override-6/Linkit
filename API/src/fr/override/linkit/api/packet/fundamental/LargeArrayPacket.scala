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

package fr.`override`.linkit.api.packet.fundamental

case class LargeArrayPacket private(ranges: Array[Int], patterns: Array[AnyVal]) {

}

object LargeArrayPacket {

    /*def apply(array: Array[AnyVal]): LargeArrayPacket = {
        val patterns = ListBuffer.empty[AnyVal]
        val ranges = ListBuffer.empty[Int]

        var index = 0
        for (it <- array) {
            val nextIndex = array.indexOf(it, index + 1)

        }
        ???
    }*/

}
