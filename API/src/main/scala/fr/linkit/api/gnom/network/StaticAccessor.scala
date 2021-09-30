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

package fr.linkit.api.gnom.network

import fr.linkit.api.gnom.network.StaticAccessor.StaticAccess

trait StaticAccessor {

    val engine: Engine

    def get[T](clazz: Class[T]): StaticAccess[T]
}

object StaticAccessor {

    trait StaticAccess[T] {
        val clazz: Class[T]

        def newInstance(args: Any*): T

        def invoke[R](methodName: String, args: Any*): R
    }
}
