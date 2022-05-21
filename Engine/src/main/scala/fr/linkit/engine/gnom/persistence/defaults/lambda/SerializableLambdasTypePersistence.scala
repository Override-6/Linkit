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

import java.lang.invoke.SerializedLambda

object SerializableLambdasTypePersistence extends LambdaTypePersistence[SerializedLambda] {
    
    override def toRepresentation(lambdaObject: AnyRef): SerializedLambda = {
        val lambdaClass = lambdaObject.getClass
        val m           = try lambdaClass.getDeclaredMethod("writeReplace") catch {
            case _: NoSuchMethodException =>
                throw new NoSuchMethodException(s"Could not find 'writeReplace' instance method for serializable lambda ($lambdaClass)")
        }
        m.setAccessible(true)
        m.invoke(lambdaObject).asInstanceOf[SerializedLambda]
    }
    
    override def toLambda(representation: SerializedLambda): AnyRef = {
        val capturingClassName = representation.getCapturingClass.replace('/', '.')
        val enclosingClass     = Class.forName(capturingClassName)
        val m                  = enclosingClass.getDeclaredMethod("$deserializeLambda$", classOf[SerializedLambda])
        m.setAccessible(true)
        m.invoke(null, representation)
    }
}
