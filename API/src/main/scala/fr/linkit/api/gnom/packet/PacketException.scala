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

import fr.linkit.api.internal.system.AppException

//TODO add packet, coords and attributes parameters.
class PacketException(msg: String, cause: Throwable = null) extends AppException(msg, cause) {

}

object PacketException {

    def apply(msg: String, cause: Throwable): PacketException = new PacketException(msg, cause)
}
