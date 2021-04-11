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

package fr.linkit.api.connection.packet.serialization

trait Serializer {

    val signature: Array[Byte]

    def serialize(serializable: Any, withSignature: Boolean): Array[Byte]

    def partialSerialize(serialized: Array[Array[Byte]], toSerialize: Array[Any]): Array[Byte]

    def isSameSignature(bytes: Array[Byte]): Boolean

    def deserialize(bytes: Array[Byte]): Any

    def deserializeAll(bytes: Array[Byte]): Array[Any]


}
