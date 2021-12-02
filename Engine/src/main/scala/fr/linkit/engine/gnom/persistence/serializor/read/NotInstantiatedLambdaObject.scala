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

package fr.linkit.engine.gnom.persistence.serializor.read

import fr.linkit.api.gnom.persistence.obj.LambdaObject
import fr.linkit.engine.gnom.persistence.serializor.ObjectDeserializationException

import java.lang.invoke.SerializedLambda
import scala.util.control.NonFatal

class NotInstantiatedLambdaObject(nio: NotInstantiatedObject[SerializedLambda]) extends LambdaObject {

    private var lambdaObject: AnyRef  = _
    private var isInit      : Boolean = false

    override def value: AnyRef = {
        if (!isInit)
            initLambda()
        lambdaObject
    }

    private def initLambda(): Unit = {
        isInit = true
        try {
            val serializedLambda = nio.value
            val capturingClassName = serializedLambda.getCapturingClass.replace('/', '.')
            val enclosingClass   = Class.forName(capturingClassName)
            val m                = enclosingClass.getDeclaredMethod("$deserializeLambda$", classOf[SerializedLambda])
            m.setAccessible(true)
            lambdaObject = m.invoke(null, serializedLambda)
        } catch {
            case e: NoSuchMethodException =>
                throw new ObjectDeserializationException(s"Could not deserialize lambda object: Could not find any (valid?) static method '$$deserializeLambda$$' in lambda capturing class.", e)
            case NonFatal(e)              =>
                throw new ObjectDeserializationException(s"Could not deserialize lambda object: ${e.getMessage}", e)
        }
    }

}
