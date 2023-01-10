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

package linkit.base.api.compilation.access

import linkit.base.api.compilation.{CompilationRequest, CompilationResult}

import java.nio.file.Path

trait CompilerAccess {

    def getType: CompilerType

    def canCompileFile(file: Path): Boolean

    /**
     * This methods will compile all classes that can be compiled with the
     * implemented compiler from the request.
     *
     * @param request the [[CompilationRequest]] to process
     * @return a [[CompilationResult]] from the compilation.
     * @throws CompilerAccessException if the compilation did not completed successfully
     * */
    def compileRequest[T](request: CompilationRequest[T]): CompilationResult[T]

    def compileAll(files: Seq[Path], destination: Path, classPaths: Seq[Path]): Unit

}
