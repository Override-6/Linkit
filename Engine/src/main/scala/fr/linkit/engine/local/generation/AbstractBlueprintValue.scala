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

package fr.linkit.engine.local.generation

import fr.linkit.api.local.generation.{BlueprintInserter, BlueprintValue, LexerUtils}

abstract class AbstractBlueprintValue[A](override val name: String, blueprint: String) extends BlueprintValue[A] {

    private val positions = LexerUtils.positions('$' + name + '$', blueprint)

    override def replaceValues(inserter: BlueprintInserter, value: A): Unit = {
        val str = toClauseString(value)
        positions.foreach(inserter.insertValue(str, name, _))
    }

    def toClauseString(a: A): String

}
