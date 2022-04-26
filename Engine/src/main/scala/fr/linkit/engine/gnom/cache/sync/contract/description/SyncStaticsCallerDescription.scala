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

package fr.linkit.engine.gnom.cache.sync.contract.description

import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.network.statics.StaticsCaller

import java.lang.reflect.Method
import scala.collection.mutable

class SyncStaticsCallerDescription[A <: StaticsCaller](override val clazz: Class[A]) extends SyncObjectDescription[A](clazz) {

    lazy val callerTargetDesc = {
        val companionClass    = clazz.getClassLoader.loadClass(clazz.getName + "$")
        val callerTargetField = companionClass.getDeclaredField("staticsTarget")
        callerTargetField.setAccessible(true)
        val callerTarget = callerTargetField.get(null).asInstanceOf[Class[_]]
        SyncStaticsDescription(callerTarget)
    }

    override protected def toMethodDesc(method: Method): MethodDescription = {
        try {
            val methodNameID = method.getName.tail.replace("_", "-").toInt
            val methodDesc   = callerTargetDesc.findMethodDescription(methodNameID).get
            MethodDescription(method, this, methodDesc.methodId)
        } catch {
            case _: NumberFormatException => super.toMethodDesc(method)
        }
    }
}

object SyncStaticsCallerDescription {

    private val cache = mutable.HashMap.empty[Class[_], SyncStaticsCallerDescription[_]]

    def apply[A <: StaticsCaller](clazz: Class[_]): SyncStaticsCallerDescription[A] = {
        cache.getOrElseUpdate(clazz, {
            new SyncStaticsCallerDescription[A](clazz.asInstanceOf[Class[A]])
        }).asInstanceOf[SyncStaticsCallerDescription[A]]
    }

}
