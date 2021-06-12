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

package fr.linkit.engine.local.generation.compilation

import fr.linkit.api.local.generation.compilation.CompilationRequest
import fr.linkit.api.local.generation.compilation.access.CompilerType
import fr.linkit.engine.local.mapping.ClassMappings

import java.io.{InputStream, OutputStream}
import java.nio.file.{Files, Path, StandardOpenOption}

abstract class CompilationRequestBuilder[T] extends CompilationRequest[T] {

    /*
     * _1: The top class's package and name.
     * _2: The Class source code.
     */
    var sourceCodes: Seq[(String, String)]

    override var classPaths: Seq[Path] = {
        ClassMappings
                .getClassPaths
                .map(cp => Path.of(cp.getLocation.toURI))
    }

    override var compilerInput: InputStream = System.in

    override var compilerOutput: OutputStream = System.out

    override var compilerErrOutput: OutputStream = System.err

    override def additionalParams(cType: CompilerType): Array[String] = Array()

    override lazy val sourceCodesPaths: Seq[Path] = {
        sourceCodes.map(pair => {
            val path = Path.of(pair._1)
            val sourceCode = pair._2
            if (Files.notExists(path))
                Files.createDirectories(path.getParent)
            Files.writeString(path, sourceCode, StandardOpenOption.CREATE)
            path
        })
    }

}
