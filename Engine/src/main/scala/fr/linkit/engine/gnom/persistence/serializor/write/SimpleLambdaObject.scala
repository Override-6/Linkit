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

package fr.linkit.engine.gnom.persistence.serializor.write

import fr.linkit.api.gnom.persistence.obj.LambdaObject

class SimpleLambdaObject(lambdaObject: AnyRef,
                         val representationDecomposed: Array[Any],
                         val representation: AnyRef) extends LambdaObject {

    override def value: AnyRef = representation

    override def identity: Int = System.identityHashCode(lambdaObject)

    override def equals(obj: Any): Boolean = obj == lambdaObject

    override def hashCode(): Int = lambdaObject.hashCode()

}

object SimpleLambdaObject {

}