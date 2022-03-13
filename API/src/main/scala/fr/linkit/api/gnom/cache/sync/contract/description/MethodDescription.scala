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

package fr.linkit.api.gnom.cache.sync.contract.description

import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription.computeID

import java.lang.reflect.{InaccessibleObjectException, Method, Parameter}

case class MethodDescription(javaMethod: Method,
                             classDesc: SyncStructureDescription[_ <: AnyRef]) {
    //TODO native method that cals any method reflectively; this is a fast fix.
    try {
        javaMethod.setAccessible(true)
    } catch {
        case _: InaccessibleObjectException => //do nothing
    }

    def getName: String = javaMethod.getName

    val params  : Array[Parameter] = javaMethod.getParameters
    val methodId: Int              = computeID(javaMethod.getName, javaMethod.getParameterTypes, javaMethod.getReturnType)
}

object MethodDescription {

    def computeID(name: String, params: Array[Class[_]], returnType: Class[_]): Int = {
        val parameters: Array[Class[_]] = params
        name.hashCode + hashCode(parameters) + returnType.getName.hashCode
    }

    private def hashCode(a: Array[Class[_]]): Int = {
        if (a == null) return 0
        var result = 1
        for (tpe <- a) {
            result = 31 * result +
                    (if (tpe == null) 0
                    else tpe.getTypeName.hashCode)
        }
        result
    }
}