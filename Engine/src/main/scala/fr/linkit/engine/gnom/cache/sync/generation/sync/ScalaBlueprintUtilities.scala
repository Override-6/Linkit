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

package fr.linkit.engine.gnom.cache.sync.generation.sync

import fr.linkit.api.gnom.cache.sync.contract.description.{MethodDescription, SyncStructureDescription}

import java.lang.reflect.TypeVariable

object ScalaBlueprintUtilities {

    def getGenericParams(desc: SyncStructureDescription[_], transform: TypeVariable[_] => Any): String = {
        val result = desc
                .clazz
                .getTypeParameters
                .map(transform)
                .mkString(",")
        if (result.isEmpty) ""
        else s"[$result]"
    }

    def getReturnType(desc: MethodDescription): String = {
        toScalaString(desc.javaMethod.getReturnType)
    }

    private def toScalaString(clazz: Class[_]): String = {
        if (clazz.isArray) {
            return s"Array[${toScalaString(clazz.componentType())}]"
        }
        val name = {
            val fullName   = clazz.getTypeName
            val simpleName = clazz.getSimpleName
            fullName.dropRight(simpleName.length).replace("$", ".") + simpleName
        }
        if (clazz.isPrimitive) {
            name match {
                case "void" => "Unit"
                case _      => name(0).toUpper + name.drop(1)
            }
        } else {
            val tParams = clazz.getTypeParameters
            if (tParams.nonEmpty) name + tParams.map(_ => "Nothing").mkString("[", ",", "]")
            else name
        }
    }

    def getParameters(desc: MethodDescription, withTypes: Boolean): String = {
        desc.javaMethod.getParameterTypes
                .zipWithIndex
                .map(pair => s"arg${
                    pair._2 + 1
                }" + (if (withTypes) ": " + toScalaString(pair._1) else ""))
                .mkString(", ")
    }

}
