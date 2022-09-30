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

package fr.linkit.engine.internal.compilation.access

import fr.linkit.api.internal.compilation.access.{CompilerAccess, CompilerAccessException}
import fr.linkit.api.internal.compilation.{CompilationRequest, CompilationResult}
import fr.linkit.engine.internal.compilation.NoResult

import java.nio.file.Path

abstract class AbstractCompilerAccess extends CompilerAccess {

    override def compileRequest[T](request: CompilationRequest[T]): CompilationResult[T] = {
        val files       = request.sourceCodesPaths
                .filter(canCompileFile)
        if (files.isEmpty)
            return NoResult(request)
        val t0          = System.currentTimeMillis()
        val outputFiles = try {
            compile(files, request.classDir, request.classPaths, request.additionalParams(getType))
        } catch {
            case e: Throwable =>
                throw new CompilerAccessException(s"Compilation went wrong: An exception occurred. (${getType.name} compiler for ${getType.languageName} language.)", e)
        }
        val t1          = System.currentTimeMillis()
        request.conclude(outputFiles, t1 - t0)
    }

    override def compileAll(files: Seq[Path], destination: Path, classPaths: Seq[Path]): Unit = {
        compile(files.filter(canCompileFile), destination, classPaths, Seq.empty)
    }

    /**
     * This method must only care about the compiler manipulation.
     * Any "unexpected" exceptions or checks are handled before this method call.
     *
     * @param sourceFiles the source files to compile
     * @param destination the destination path where all compiled files must be put.
     *                    The relative source file's package directory must be the same as it's compiled file package directory from the
     *                    destination path folder
     * @param classPaths all classPaths to include during the compilation.
     * @param additionalArguments additional compiler's arguments.
     * @return a sequence of compiled paths.
     * */
    protected def compile(sourceFiles: Seq[Path], destination: Path, classPaths: Seq[Path], additionalArguments: Seq[String]): Seq[Path]

}
