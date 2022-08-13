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

package fr.linkit.api.internal.generation

import java.lang.reflect.{GenericDeclaration, Type, TypeVariable}
import java.util.function.IntFunction
import java.util.regex.{MatchResult, Pattern}

object TypeVariableTranslator {

    private val JavaArraySelectorPattern = Pattern.compile("([^<>\\[\\],\\s]*)\\[]")
    private val TypeDotsSelectorPattern  = Pattern.compile("(\\.)]")

    private val ScalaMappings = Seq(
        "<" -> "[",
        ">" -> "]",
        "?" -> "_",
        "extends" -> "<:",
        "super" -> ">:",
        "&" -> "with"
    )

    def toJavaDeclaration(types: Array[_ <: TypeVariable[_ <: GenericDeclaration]]): String = {
        toDeclaration(types)
    }

    def toScalaDeclaration(typ: Type): String = getScalaPrimitiveNameEquivalent(typ).getOrElse {
        var scalaDeclaration = typ.getTypeName
        for ((j, s) <- ScalaMappings) {
            scalaDeclaration = scalaDeclaration.replace(j, s)
        }
        translateToScalaArrays {
            fixDots(scalaDeclaration)
        }
    }

    private def fixDots(str: String): String = {
        val matcher = TypeDotsSelectorPattern
            .matcher(str)
        if (matcher.matches()) {
            matcher.replaceAll("$[")
        }
        str
    }

    def getScalaPrimitiveNameEquivalent(typ: Type): Option[String] = {
        val v = typ.getTypeName match {
            case "void"    => "Unit"
            case "null"    => "null"
            case "int"     => "Int"
            case "boolean" => "Boolean"
            case "short"   => "Short"
            case "long"    => "Long"
            case "double"  => "Double"
            case "float"   => "Float"
            case "char"    => "Char"
            case _         => null
        }
        Option(v)
    }

    def translateToScalaArrays(str: String): String = {
        val matcher = JavaArraySelectorPattern.matcher(str)
        matcher.replaceAll((matchResult: MatchResult) => {
            s"Array[${matchResult.group(1)}]"
        })
    }

    def toScalaDeclaration(types: Array[_ <: TypeVariable[_ <: GenericDeclaration]]): String = {
        var scalaDeclaration = toDeclaration(types)
        for ((j, s) <- ScalaMappings) {
            scalaDeclaration = scalaDeclaration.replace(j, s)
        }
        translateToScalaArrays {
            scalaDeclaration
        }
    }

    private def toDeclaration(types: Array[_ <: TypeVariable[_ <: GenericDeclaration]]): String = {
        val builder = new StringBuilder
        types.foreach { typeVar =>
            builder.append(toJavaGenericParameter(typeVar))
                .append(",")
        }
        fixDots {
            builder
                .dropRight(2) //Removes ", "
                .toString()
        }
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
                val name   = bound.getTypeName
                builder
                    .append(prefix)
                    .append(name)
            }
        builder.toString()
    }

}
