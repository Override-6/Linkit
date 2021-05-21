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

import fr.linkit.api.local.generation.{JavaClassBlueprint, ValueScope}
import fr.linkit.engine.local.generation.SimpleJavaClassBlueprint.removeBPComments

import java.io.InputStream

class SimpleJavaClassBlueprint[V] private(blueprint: String, rootScope: ValueScope[V]) extends JavaClassBlueprint[V] {

    def this(blueprint: String, rootProvider: String => ValueScope[V]) = {
        this(blueprint, rootProvider(removeBPComments(blueprint)))
    }

    def this(stream: InputStream, rootProvider: String => ValueScope[V]) = {
        this(new String(stream.readAllBytes()), rootProvider)
    }

    override def getBlueprintString: String = blueprint

    override def toClassSource(v: V): String = rootScope.getSourceCode(v)

}

object SimpleJavaClassBlueprint {

    val BlueprintCommentPrefix: String = "//#"

    def removeBPComments(blueprint: String): String = {
        var nextCommentPos = blueprint.indexOf(BlueprintCommentPrefix)
        val result         = new StringBuilder(blueprint)

        def shouldDeleteWholeLine(from: Int): Boolean = {
            for (i <- from to nextCommentPos) {
                if (!result(i).isWhitespace)
                    return false
            }
            true
        }

        while (nextCommentPos != -1) {
            val commentEndPos = result.indexOf('\n', nextCommentPos)
            val lineStartPos = result.lastIndexOf('\n', nextCommentPos)
            result.delete(nextCommentPos, commentEndPos)
            if (shouldDeleteWholeLine(lineStartPos)) {
                result.delete(lineStartPos, nextCommentPos)
            }
            nextCommentPos = result.indexOf(BlueprintCommentPrefix, nextCommentPos)
        }
        result.toString()
    }
}