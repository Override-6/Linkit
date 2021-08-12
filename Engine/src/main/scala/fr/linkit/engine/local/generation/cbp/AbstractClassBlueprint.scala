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

package fr.linkit.engine.local.generation.cbp

import fr.linkit.api.local.generation.cbp.ClassBlueprint
import fr.linkit.api.local.generation.compilation.CompilationContext
import fr.linkit.engine.local.generation.cbp.AbstractClassBlueprint.removeBPComments

import java.io.InputStream

abstract class AbstractClassBlueprint[V <: CompilationContext] private(protected val blueprint: String) extends ClassBlueprint[V] {

    def this(stream: InputStream) = {
        this(removeBPComments(new String(stream.readAllBytes())))
    }

    val rootScope: RootValueScope

    override def getBlueprintString: String = blueprint

    override def toClassSource(v: V): String = rootScope.getSourceCode(v)

    abstract class RootValueScope extends AbstractValueScope[V]("ROOT", 0, blueprint) {
        bindValue("CompileTime" ~~> System.currentTimeMillis())
        bindValue("ClassName" ~> (_.className))
        bindValue("ClassPackage" ~> (_.classPackage))
    }

    object RootValueScope {

        def apply(other: AbstractValueScope[V]): RootValueScope = new RootValueScope {
            bindAll(other)
        }
    }

}

object AbstractClassBlueprint {

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
            val lineStartPos  = result.lastIndexOf('\n', nextCommentPos)
            result.delete(nextCommentPos, commentEndPos)
            if (shouldDeleteWholeLine(lineStartPos)) {
                result.delete(lineStartPos, nextCommentPos)
            }
            nextCommentPos = result.indexOf(BlueprintCommentPrefix, nextCommentPos)
        }
        result.toString()
    }
}