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

import fr.linkit.api.gnom.persistence.context.{ControlBox, TypePersistence}
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.engine.gnom.persistence.config.structure.ArrayObjectStructure
import fr.linkit.engine.internal.utils.ScalaUtils

import java.lang.invoke.SerializedLambda

object SerializableLambdasTypePersistence extends TypePersistence[AnyRef] {
    
    override val structure: ObjectStructure = ArrayObjectStructure(classOf[SerializedLambda])
    
    override def initInstance(allocatedObject: AnyRef, args: Array[Any], box: ControlBox): Unit = {
        val representation     = args.head.asInstanceOf[SerializedLambda]
        val capturingClassName = representation.getCapturingClass.replace('/', '.')
        val enclosingClass     = Class.forName(capturingClassName)
        val m                  = enclosingClass.getDeclaredMethod("$deserializeLambda$", classOf[SerializedLambda])
        m.setAccessible(true)
        ScalaUtils.pasteAllFields(allocatedObject, m.invoke(null, representation))
    }
    
    override def toArray(lambdaObject: AnyRef): Array[Any] = {
        val lambdaClass = lambdaObject.getClass
        val m           = try lambdaClass.getDeclaredMethod("writeReplace") catch {
            case _: NoSuchMethodException =>
                throw new NoSuchMethodException(s"Could not find 'writeReplace' instance method for serializable lambda ($lambdaClass)")
        }
        m.setAccessible(true)
        Array(m.invoke(lambdaObject))
    }
    
}
