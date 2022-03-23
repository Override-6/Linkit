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

package fr.linkit.engine.gnom.persistence.defaults.lambda

import fr.linkit.api.gnom.persistence.context.LambdaTypePersistence
import fr.linkit.engine.internal.manipulation.creation.ObjectCreator

import java.lang.invoke.MethodHandles

//TODO  https://stackoverflow.com/questions/23595136/get-a-list-of-classes-lambdas
object NotSerializableLambdasTypePersistence extends LambdaTypePersistence[JLTPRepresentation] {

    //private val lookup = MethodHandles.lookup()

    override def toRepresentation(lambdaObject: AnyRef): JLTPRepresentation = {
        throw new UnsupportedOperationException("Can't serialize not serializable lambdas")
        val clazz = lambdaObject.getClass
        val name = clazz.getName
        val enclosingClass = name.take(name.indexOf("$$Lambda$"))
        val decomposed = ObjectCreator.getAllFields(lambdaObject, clazz.getDeclaredFields)
        JLTPRepresentation(enclosingClass, decomposed.asInstanceOf[Array[Any]])
    }

    override def toLambda(representation: JLTPRepresentation): AnyRef = {
        val args = representation.lambdaParameters
        val enclosing = Class.forName(representation.enclosingClass)
        //val mtype = MethodType.methodType()
        ???
    }
}

case class JLTPRepresentation(enclosingClass: String, lambdaParameters: Array[Any])