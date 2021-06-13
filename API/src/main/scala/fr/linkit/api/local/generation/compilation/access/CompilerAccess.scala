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

package fr.linkit.api.local.generation.compilation.access

import fr.linkit.api.local.generation.compilation.{CompilationRequest, CompilationResult}

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
