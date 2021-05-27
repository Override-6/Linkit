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

package fr.linkit.engine.connection.cache.repo.generation

import java.lang.reflect.{GenericDeclaration, TypeVariable}

object TypeVariableTranslator {

    def toJavaDeclaration(types: Array[_ <: TypeVariable[_ <: GenericDeclaration]]): String = {
        val builder = new StringBuilder
        types.foreach { typeVar =>
            builder.append(toJavaGenericParameter(typeVar))
                    .append(", ")
        }
        builder
                .dropRight(2) //Removes ", "
                .toString()
    }

    def toScalaDeclaration(types: Array[_ <: TypeVariable[_ <: GenericDeclaration]]): String = {
        toJavaDeclaration(types)
                .replace('<', '[')
                .replace('>', ']')
                .replace('?', '_')
                .replace("extends", "<:")
                .replace("$", "with")
    }

    private def toJavaGenericParameter(typeVar: TypeVariable[_ <: GenericDeclaration]): String = {
        val builder   = new StringBuilder(typeVar.getTypeName)
        var firstType = true
        typeVar.getBounds
                .filter(_ != classOf[Object])
                .foreach { bound =>
            val prefix = if (firstType) {
                firstType = false
                " extends "
            } else " & "
            builder
                    .append(prefix)
                    .append(bound.getTypeName)
        }
        builder.toString()
    }

}
