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

package fr.linkit.engine.connection.cache.repo.generation

import fr.linkit.api.connection.cache.repo.generation.CompilerAccess

import java.nio.file.{Files, Path}

abstract class AbstractCompilerAccess extends CompilerAccess {

    override def compileAll(sourceFolder: Path, destination: Path, classPaths: Seq[Path]): Int = {
        val sources = listSources(sourceFolder)
        compileAll(sources, destination, classPaths)
    }

    override def compileAll(sourceFiles: Array[Path], destination: Path, classPaths: Seq[Path]): Int = {
        compile(sourceFiles.filter(filePredicate), destination, classPaths)
    }

    def filePredicate(filePath: Path): Boolean

    def compile(sourceFiles: Array[Path], destination: Path, classPaths: Seq[Path]): Int

    private def listSources(path: Path): Array[Path] = {
        Files.list(path)
                .toArray(new Array[Path](_))
                .flatMap(subPath => {
                    if (Files.isDirectory(subPath))
                        listSources(subPath)
                    else Array(subPath)
                })
    }

}
