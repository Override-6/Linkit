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

package fr.linkit.engine.local.generation.compilation.access

import fr.linkit.api.local.generation.compilation.access.{CompilerAccess, CompilerAccessException}
import fr.linkit.api.local.generation.compilation.{CompilationRequest, CompilationResult}

import java.nio.file.Path
import scala.util.control.NonFatal

abstract class AbstractCompilerAccess extends CompilerAccess {

    override def compileRequest[T](request: CompilationRequest[T]): CompilationResult[T] = {
        val files       = request.sourceCodesPaths
                .filter(canCompileFile)
        val t0          = System.currentTimeMillis()
        val outputFiles = try {
            compile(files, request.classDir, request.classPaths, request.additionalParams(getType))
        } catch {
            case NonFatal(e) =>
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
