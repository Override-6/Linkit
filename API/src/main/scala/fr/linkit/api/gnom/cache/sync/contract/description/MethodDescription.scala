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

package fr.linkit.api.gnom.cache.sync.contract.description

import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription.computeID

import java.lang.reflect.{InaccessibleObjectException, Method, Parameter}

case class MethodDescription(javaMethod: Method,
                             classDesc: SyncStructureDescription[_ <: AnyRef],
                             methodId: Int) {

    def this(javaMethod: Method, classDesc: SyncStructureDescription[_ <: AnyRef]) = {
        this(javaMethod, classDesc, computeID(javaMethod))
    }

    val isMethodAccessible = try {
        javaMethod.setAccessible(true)
        true
    } catch {
        case _: InaccessibleObjectException => false
    }
    
    def getName: String = javaMethod.getName

    val params  : Array[Parameter] = javaMethod.getParameters
}

object MethodDescription {

    def computeID(name: String, params: Array[Class[_]], returnType: Class[_]): Int = {
        val parameters: Array[Class[_]] = params
        name.hashCode + hashCode(parameters) + returnType.getName.hashCode
    }

    def computeID(method: Method): Int = computeID(method.getName, method.getParameterTypes, method.getReturnType)

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