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

package fr.linkit.api.internal.system.security

trait BytesHasher {

    def hashBytes(raw: Array[Byte]): Array[Byte]

    def deHashBytes(hashed: Array[Byte]): Array[Byte]

    val key: String

    val signature: Array[Byte]

}

object BytesHasher {

    class Inactive extends BytesHasher {

        override def hashBytes(raw: Array[Byte]): Array[Byte] = raw

        override def deHashBytes(hashed: Array[Byte]): Array[Byte] = hashed

        override val key: String = "inactive-hasher"

        override val signature: Array[Byte] = Array(1)
    }

    def inactive: BytesHasher = new Inactive
}