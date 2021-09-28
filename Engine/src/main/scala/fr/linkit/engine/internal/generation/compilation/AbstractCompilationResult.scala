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

package fr.linkit.engine.internal.generation.compilation

import fr.linkit.api.internal.generation.compilation.{CompilationRequest, CompilationResult}

import java.nio.file.Path

abstract class AbstractCompilationResult[T](outs: Seq[Path], compileTime: Long, request: CompilationRequest[_]) extends CompilationResult[T] {

    override def getOuterFiles: Seq[Path] = outs

    override def getCompileTime: Long = compileTime

    override def getRequest: CompilationRequest[_] = request
}
