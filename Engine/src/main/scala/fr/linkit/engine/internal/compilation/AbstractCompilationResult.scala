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

package fr.linkit.engine.internal.compilation

import fr.linkit.api.internal.compilation.{CompilationRequest, CompilationResult}

import java.nio.file.Path

abstract class AbstractCompilationResult[T](outs: Seq[Path], compileTime: Long, request: CompilationRequest[_]) extends CompilationResult[T] {

    override def getOuterFiles: Seq[Path] = outs

    override def getCompileTime: Long = compileTime

    override def getRequest: CompilationRequest[_] = request
}
