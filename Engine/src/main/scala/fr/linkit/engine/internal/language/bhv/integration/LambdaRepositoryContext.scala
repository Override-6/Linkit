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

package fr.linkit.engine.internal.language.bhv.integration

import fr.linkit.api.internal.generation.compilation.CompilationContext

import java.io.File

case class LambdaRepositoryContext(fileName: String,
                                   expressions: Array[LambdaExpressionInfo],
                                   blocks: Array[String],
                                   parentLoader: ClassLoader,
                                   importedClasses: Seq[Class[_]]) extends CompilationContext {

    override def className: String = s"LambdaRepository_${fileName.replace(File.separator, "$")}"

    override def classPackage: String = "gen.scala.bhv.expression"

}
