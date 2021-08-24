/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.api.connection.cache.obj.description

import fr.linkit.api.connection.cache.obj.description.MethodDescription.NumberTypes

import java.lang.reflect.Method

case class MethodDescription(method: Method,
                             classDesc: SyncObjectSuperclassDescription[_]) {

    val methodId: Int = {
        val parameters: Array[Class[_]] = method.getParameterTypes
        method.getName.hashCode + hashCode(parameters)
    }

    def getDefaultTypeReturnValue: String = {
        val nme = method.getReturnType.getName

        if (nme == fullNameOf[Boolean]) "false"
        else if (NumberTypes.contains(fullNameOf)) "-1"
        else if (nme == fullNameOf[Char]) "'\\u0000'"
        else "nl()" //contracted call to JavaUtils.getNull
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

object MethodDescription {

    val NumberTypes: Array[String] = Array(fullNameOf[Float], fullNameOf[Double], fullNameOf[Int], fullNameOf[Byte], fullNameOf[Long], fullNameOf[Short])
}