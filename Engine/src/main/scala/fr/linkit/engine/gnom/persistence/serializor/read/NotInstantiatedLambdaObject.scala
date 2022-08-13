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

package fr.linkit.engine.gnom.persistence.serializor.read

import fr.linkit.api.gnom.persistence.context.LambdaTypePersistence
import fr.linkit.api.gnom.persistence.obj.LambdaObject
import fr.linkit.engine.gnom.persistence.serializor.ObjectDeserializationException

import scala.util.control.NonFatal

class NotInstantiatedLambdaObject(nio: NotInstantiatedObject[AnyRef],
                                  persistence: LambdaTypePersistence[AnyRef]) extends LambdaObject {

    private var lambdaObject: AnyRef  = _
    private var isInit      : Boolean = false

    override def value: AnyRef = {
        if (!isInit)
            initLambda()
        lambdaObject
    }

    override def identity: Int = nio.identity

    private def initLambda(): Unit = {
        isInit = true
        try {
            val representation = nio.value
            lambdaObject = persistence.toLambda(representation)
        } catch {
            case e: NoSuchMethodException =>
                throw new ObjectDeserializationException(s"Could not deserialize lambda object: Could not find any (valid?) static method '$$deserializeLambda$$' in lambda capturing class.", e)
            case NonFatal(e)              =>
                throw new ObjectDeserializationException(s"Could not deserialize lambda object: ${e.getMessage}", e)
        }
    }
}
