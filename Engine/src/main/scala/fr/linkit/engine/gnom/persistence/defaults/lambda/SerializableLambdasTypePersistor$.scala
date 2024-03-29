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

package fr.linkit.engine.gnom.persistence.defaults.lambda

import fr.linkit.api.gnom.persistence.context.{ControlBox, Decomposition, ObjectTranform, TypePersistor}
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.engine.gnom.persistence.config.structure.ArrayObjectStructure
import fr.linkit.engine.internal.util.ScalaUtils

import java.lang.invoke.SerializedLambda

object SerializableLambdasTypePersistor$ extends TypePersistor[AnyRef] {
    
    override val structure: ObjectStructure = ArrayObjectStructure(classOf[SerializedLambda])
    
    override def initInstance(allocatedObject: AnyRef, args: Array[Any], box: ControlBox): Unit = {
        val representation     = args.head.asInstanceOf[SerializedLambda]
        val capturingClassName = representation.getCapturingClass.replace('/', '.')
        val enclosingClass     = Class.forName(capturingClassName)
        val m                  = enclosingClass.getDeclaredMethod("$deserializeLambda$", classOf[SerializedLambda])
        m.setAccessible(true)
        ScalaUtils.pasteAllFields(allocatedObject, m.invoke(null, representation))
    }
    
    override def transform(lambdaObject: AnyRef): ObjectTranform = {
        val lambdaClass = lambdaObject.getClass
        val m           = try lambdaClass.getDeclaredMethod("writeReplace") catch {
            case _: NoSuchMethodException =>
                throw new NoSuchMethodException(s"Could not find 'writeReplace' instance method for serializable lambda ($lambdaClass)")
        }
        m.setAccessible(true)
        Decomposition(Array(m.invoke(lambdaObject)))
    }
    
}
