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
import fr.linkit.engine.local.generation.compilation.SourceCodeCompilationRequest.SourceCode
import fr.linkit.engine.local.mapping.ClassMappings

import java.io.File
import java.nio.file.{Files, Path, StandardOpenOption}

abstract class SourceCodeCompilationRequest[T] extends CompilationRequest[T] {

    /*
     * _1: The top class's package and name.
     * _2: The Class source code.
     */
    var sourceCodes: Seq[SourceCode]

    override val classPaths: Seq[Path] = {
        ClassMappings
                .getClassPaths
                .map(cp => Path.of(cp.getLocation.toURI))
    }

    override def additionalParams(cType: CompilerType): Array[String] = Array()

    override lazy val sourceCodesPaths: Seq[Path] = {
        sourceCodes.map(sc => {
            val path = sourcesDir.resolve(sc.className.replace(".", File.separator) + sc.codeType.sourceFileExtension)
            val sourceCode = sc.sourceCode
            if (Files.notExists(path))
                Files.createDirectories(path.getParent)
            Files.writeString(path, sourceCode, StandardOpenOption.CREATE)
            path
        })
    }
}

object SourceCodeCompilationRequest {
    case class SourceCode(className: String, sourceCode: String, codeType: CompilerType)

    implicit class SourceCodeHelper(className: String) {
        def ~> (sourceCode: String, codeType: CompilerType): SourceCode = SourceCode(className, sourceCode, codeType)
    }
}
