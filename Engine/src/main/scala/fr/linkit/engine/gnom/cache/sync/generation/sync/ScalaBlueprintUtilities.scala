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

package fr.linkit.engine.gnom.cache.sync.generation.sync

import fr.linkit.api.gnom.cache.sync.contract.description.{MethodDescription, SyncStructureDescription}

import java.lang.reflect.{Modifier, TypeVariable}
import scala.collection.immutable.HashSet

object ScalaBlueprintUtilities {

    def getGenericParams(desc: SyncStructureDescription[_], transform: TypeVariable[_] => Any): String = {
        val specs  = desc.specs
        val main   = specs.mainClass
        val result = main
                .getTypeParameters
                .map(transform)
                .mkString(",")
        if (result.isEmpty) ""
        else s"[$result]"
    }

    def getReturnType(desc: MethodDescription): String = {
        toScalaString(desc.javaMethod.getReturnType)
    }

    def toScalaString(clazz: Class[_]): String = {
        if (clazz.isArray) {
            return s"Array[${toScalaString(clazz.componentType())}]"
        }
        val name   = {
            val fullName   = clazz.getTypeName
            val simpleName = clazz.getSimpleName
            val s          = fullName.dropRight(simpleName.length)
                    .replace("$", ".")
            if (s.lastOption.contains('.')) s.dropRight(1) + (if (isInnerClass(clazz)) "#" else ".") + simpleName
            else s + simpleName
        }
        val result = if (clazz.isPrimitive) {
            name match {
                case "void" => "Unit"
                case _      => name(0).toUpper + name.drop(1)
            }
        } else {
            val tParams = clazz.getTypeParameters
            if (tParams.nonEmpty) name + tParams.map(_ => "Nothing").mkString("[", ",", "]")
            else name
        }
        val r      = result.split('.').map(fixClashWord).mkString(".")
        r
    }

    def isInnerClass(clazz: Class[_]): Boolean = {
        val enclosing = clazz.getEnclosingClass
        enclosing != null && !Modifier.isStatic(clazz.getModifiers)
    }

    private final val keyWords = HashSet("case", "else", "type", "if", "class", "def", "val", "var", "match", "for", "import", "package", "object", "trait", "sealed", "true", "false", "override", "catch", "forSome", "package", "try", "private", "protected", "lazy", "while", "extends", "with", "this", "super", "yield", "final", "finally", "null", "throw")

    private def fixClashWord(word: String): String = {
        if (keyWords.contains(word)) s"`$word`"
        else word
    }

    def getParameters(desc: MethodDescription, withTypes: Boolean, withVarargsInlines: Boolean): String = {
        val method = desc.javaMethod
        if (withTypes && method.getName == "equals" &&
                (method.getParameterTypes sameElements Array(classOf[Object])) &&
                method.getReturnType == java.lang.Boolean.TYPE)
            return "arg1: scala.Any" //java.lang.Object method needs a special return
        val params = method.getParameters
        params.zipWithIndex
                .map { case (param, idx) => s"arg${idx + 1}" + (if (withTypes) ": " + toScalaString(param.getType)
                else if (withVarargsInlines && param.isVarArgs) ":_*" else "")
                }
                .mkString(", ")
    }

    def getParameters(params: Array[Class[_]], withTypes: Boolean): String = {
        params.zipWithIndex
                .map { case (clazz, idx) => s"arg${idx + 1}" + (if (withTypes) ": " + toScalaString(clazz) else "") }
                .mkString(", ")
    }

}
