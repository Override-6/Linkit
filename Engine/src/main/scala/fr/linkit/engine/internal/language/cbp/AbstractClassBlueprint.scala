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

package fr.linkit.engine.internal.language.cbp

import fr.linkit.api.internal.generation.compilation.CompilationContext
import fr.linkit.api.internal.language.cbp.ClassBlueprint
import fr.linkit.engine.internal.language.cbp.AbstractClassBlueprint.removeBPComments

import java.io.InputStream
import scala.collection.mutable

abstract class AbstractClassBlueprint[V <: CompilationContext] private(protected val blueprint: String) extends ClassBlueprint[V] {

    def this(stream: InputStream) = {
        this(removeBPComments(new String(stream.readAllBytes())))
    }

    val rootScope: RootValueScope

    override def getBlueprintString: String = blueprint

    override def toClassSource(v: V): String = rootScope.getSourceCode(v)

    abstract class RootValueScope extends AbstractValueScope[V]("ROOT", blueprint, 0) {
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
        val result         = new mutable.StringBuilder(blueprint)

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