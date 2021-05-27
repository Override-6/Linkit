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

object ByteCodeGenericParameterTranslator {

    def toJavaDeclaration(expression: String): String = {
        val builder = new StringBuilder("<")
        if (!expression.startsWith("<") && !expression.endsWith(">"))
            throw new IllegalArgumentException("Expression must start with '<' and end with '>'.")
        val pureExpression = expression.drop(1).dropRight(1)
        if (pureExpression.isEmpty)
            throw new IllegalArgumentException("Pure expression is empty.")
        var index = 0
        for (x <- pureExpression.split(";[^:>]")) {
            val genParam = pureExpression.slice(index, index + x.length + 1)
            builder.append(toJavaGenericParameter(genParam))
                    .append(", ")
            index += genParam.length + (if (index == 0) 0 else 1)
        }
        builder
                .dropRight(2) //Remove trailing ", "
                .append(">")
                .toString()
    }

    def toJavaGenericParameter(genParam: String): String = {
        val name    = genParam.take(genParam.indexOf(":"))
        val builder = new StringBuilder(name)
        val isSuper = genParam.indexOf("::", name.length) == name.length
        builder.append(" extends ")

        val typeSpecifications = genParam.drop(name.length + (if (isSuper) 2 else 1)).split(":")

        typeSpecifications.foreach { typeSpec =>
            builder.append(extractTypeSpecification(typeSpec) + " & ")
        }

        builder
                .dropRight(3) //Removes trailing " & "
                .toString()
    }

    private def extractTypeSpecification(typeSpec: String): String = {
        if (typeSpec == "*")
            return "?" //It's a wildcard !
        val formatted        = {
            (if (typeSpec.endsWith(";"))
                typeSpec.dropRight(1)
            else typeSpec).drop(1) //Removing the 'L' Object flag
                    .replaceAll("/", ".")
        } //Remove the ';' flag end if contained.
        val genericTypeBegin = formatted.indexOf('<')
        if (genericTypeBegin >= 0) {
            //It's a type that contains sub type specification
            //(Such as List<V> for a Java generic parameter 'V extends List<V>')
            val subType          = formatted.slice(genericTypeBegin + 1, formatted.indexOf('>'))
            val genericFormatted = formatted.take(formatted.indexOf('<'))
            genericFormatted + "<" + extractTypeSpecification(subType) + ">"
        } else formatted
    }
}
