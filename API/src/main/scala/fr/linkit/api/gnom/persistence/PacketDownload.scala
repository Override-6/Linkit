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

package fr.linkit.api.gnom.persistence

import java.nio.ByteBuffer

trait PacketDownload extends PacketTransfer {

    val buff: ByteBuffer
    
    val ordinal: Int

    def makeDeserialization(): Unit

    def isDeserialized: Boolean

    def isInjected: Boolean

    def informInjected: Unit
}
