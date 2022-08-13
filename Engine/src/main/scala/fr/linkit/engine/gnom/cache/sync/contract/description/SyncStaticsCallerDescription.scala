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

package fr.linkit.engine.gnom.cache.sync.contract.description

import fr.linkit.api.gnom.cache.sync.contract.description.{MethodDescription, SyncClassDef, SyncClassDefMultiple, SyncClassDefUnique}
import fr.linkit.api.gnom.network.statics.StaticsCaller

import java.lang.reflect.Method
import scala.collection.mutable

class SyncStaticsCallerDescription[A <: StaticsCaller](override val specs: SyncClassDef) extends SyncObjectDescription[A](specs) {

    private lazy val callerTargetDesc = {
        if (specs.isInstanceOf[SyncClassDefMultiple])
            throw new IllegalArgumentException("class def can't be multiple")
        SyncStaticsDescription(specs.mainClass)
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
            new SyncStaticsCallerDescription[A](SyncClassDef(clazz))
        }).asInstanceOf[SyncStaticsCallerDescription[A]]
    }

}
