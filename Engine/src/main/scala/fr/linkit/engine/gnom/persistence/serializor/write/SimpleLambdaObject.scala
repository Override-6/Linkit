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
import fr.linkit.engine.internal.manipulation.creation.ObjectCreator

import java.lang.invoke.SerializedLambda
import java.lang.reflect.Modifier

class SimpleLambdaObject(val enclosingClass: Class[_], lambdaObject: AnyRef, val value: SerializedLambda) extends LambdaObject {

    val decomposed: Array[Any] = {
        val fields = value.getClass.getDeclaredFields.filterNot(f => Modifier.isStatic(f.getModifiers))
        ObjectCreator.getAllFields(value, fields).asInstanceOf[Array[Any]]
    }

    override def equals(obj: Any): Boolean = obj == lambdaObject

    override def hashCode(): Int = lambdaObject.hashCode()

}
