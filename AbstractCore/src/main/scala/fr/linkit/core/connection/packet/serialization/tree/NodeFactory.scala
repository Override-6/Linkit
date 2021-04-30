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

import fr.linkit.core.connection.packet.serialization.tree.SerialContext.ClassProfile

trait NodeFactory[T] {

    def canHandle(clazz: Class[_]): Boolean

    def canHandle(info: ByteSeq): Boolean

    def newNode(finder: SerialContext, profile: ClassProfile[T]): SerialNode[T]

    def newNode(finder: SerialContext, bytes: ByteSeq): DeserialNode[T]

}
