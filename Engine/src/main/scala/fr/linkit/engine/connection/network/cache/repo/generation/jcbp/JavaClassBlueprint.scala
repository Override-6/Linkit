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

package fr.linkit.engine.connection.network.cache.repo.generation.jcbp

import fr.linkit.engine.connection.network.cache.repo.PuppetClassDesc

import scala.collection.mutable.ListBuffer

class JavaClassBlueprint(blueprint: String) {

    private val values: ClassBlueprintValue

    def getSourceCode(classDesc: PuppetClassDesc): String = {
        val builder = new StringBuilder(blueprint)
        val sourceBuilder = new SourceCodeBuilder(builder)

    }

}

object JavaClassBlueprint {

    class SourceCodeBuilder(blueprint: StringBuilder) {

        private var posShift = 0

        def replaceValue(valueName: String, value: String, pos: Int): Unit = {
            val valueNameLength = valueName.length + 2
            blueprint.replace(pos + posShift, valueNameLength, value)
            posShift += value.length - valueNameLength
        }

    }

    class ClassBlueprintValue(name: String, blueprint: String) {

        private val locations = positions('$' + name + '$', blueprint)

        def formatAll(scBuilder: SourceCodeBuilder, value: String): Unit = {
            locations.foreach(pos => scBuilder.replaceValue(name, value, pos))
        }
    }

    private def positions(str: String, blueprint: String): Seq[Int] = {
        var lastIndex = blueprint.indexOf(str)
        val locations = ListBuffer.empty[Int]
        while (lastIndex != -1) {
            locations += lastIndex
            lastIndex = blueprint.indexOf(str, lastIndex)
        }
        locations.toSeq
    }

}
