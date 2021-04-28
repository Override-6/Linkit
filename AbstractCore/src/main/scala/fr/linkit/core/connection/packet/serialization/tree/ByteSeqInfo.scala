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

package fr.linkit.core.connection.packet.serialization.tree

import fr.linkit.core.local.mapping.ClassMappings
import fr.linkit.core.local.utils.NumberSerializer

case class ByteSeqInfo(bytes: Array[Byte]) {

    lazy val classType: Option[Class[_]] = {
        if (bytes.length < 4)
            None
        else
            ClassMappings.getClassOpt(NumberSerializer.deserializeInt(bytes, 0))
    }

    def isClassDefined: Boolean = classType.isDefined

    def classExists(f: Class[_] => Boolean): Boolean = classType exists f

    def sameFlag(flag: Byte): Boolean = bytes.nonEmpty && bytes(0) == flag
}
