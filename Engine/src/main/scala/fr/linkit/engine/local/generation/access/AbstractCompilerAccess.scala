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

package fr.linkit.engine.local.generation.access

import fr.linkit.api.local.generation.{CompilerAccess, CompilerAccessException}

import java.nio.file.{Files, Path}
import scala.util.control.NonFatal

abstract class AbstractCompilerAccess extends CompilerAccess {

    override def compileAll(sourceFolder: Path, destination: Path, classPaths: Seq[Path]): Int = {
        val sources = listSources(sourceFolder)
        compileAll(sources, destination, classPaths)
    }

    override def compileAll(sourceFiles: Array[Path], destination: Path, classPaths: Seq[Path]): Int = {
        if (sourceFiles.exists(!canCompileFile(_)))
            throw new IllegalArgumentException(s"Provided source files array contains files that can't be compiled by ${getType.name}")
        val code = try {
            compile(sourceFiles, destination, classPaths)
        } catch {
            case NonFatal(e) =>
                throw new CompilerAccessException(s"Compilation went wrong: An exception occurred. (${getType.name} compiler for ${getType.languageName} language.)", -155, e)
        }
        if (code != 0)
            throw new CompilerAccessException(s"Compilation went wrong: Exit code is not 0. (${getType.name} compiler for ${getType.languageName} language.)", code)
        code
    }

    protected def compile(sourceFiles: Array[Path], destination: Path, classPaths: Seq[Path]): Int

    private def listSources(path: Path): Array[Path] = {
        Files.list(path)
            .toArray(new Array[Path](_))
            .flatMap(subPath => {
                if (Files.isDirectory(subPath))
                    listSources(subPath)
                else Array(subPath)
            })
            .filter(canCompileFile)
    }

}
