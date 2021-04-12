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

package fr.linkit.core.connection.packet.serialization.v2

import scala.collection.mutable.ListBuffer

class SimpleAdaptiveSerializer extends AdaptiveSerializer {

    private val alternatives = ListBuffer.empty[SerialAlternative[_]]

    override def attachAlternative(alternative: SerialAlternative[_]): Unit = alternatives += alternative

    override val signature: Array[Byte] = Array(50)

    override def serialize(serializable: Any, withSignature: Boolean): Array[Byte] = ???

    override def partialSerialize(serialized: Array[Array[Byte]], toSerialize: Array[Any]): Array[Byte] = ???

    override def isSameSignature(bytes: Array[Byte]): Boolean = ???

    override def deserialize(bytes: Array[Byte]): Any = ???

    override def deserializeAll(bytes: Array[Byte]): Array[Any] = ???
}
