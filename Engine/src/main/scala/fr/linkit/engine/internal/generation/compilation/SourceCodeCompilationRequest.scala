/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.internal.generation.compilation

import fr.linkit.api.internal.language.cbp.ClassBlueprint
import fr.linkit.api.internal.generation.compilation.access.CompilerType
import fr.linkit.api.internal.generation.compilation.{CompilationContext, CompilationRequest}
import fr.linkit.engine.internal.generation.compilation.SourceCodeCompilationRequest.SourceCode
import fr.linkit.engine.internal.mapping.ClassMappings

import java.io.{File, InputStream, OutputStream}
import java.nio.file.{Files, Path, StandardOpenOption}

/**
 * @tparam R the Request result.
 * */
abstract class SourceCodeCompilationRequest[R] extends CompilationRequest[R] {

    var sourceCodes: Seq[SourceCode]

    protected final val defaultClassPaths: Seq[Path] = {
        ClassMappings
                .getClassPaths
                .map(cp => Path.of(cp.getLocation.toURI))
    }

    override val classPaths: Seq[Path] = defaultClassPaths

    override def additionalParams(cType: CompilerType): Array[String] = Array()

    override lazy val sourceCodesPaths: Seq[Path] = {
        sourceCodes.map(sc => {
            val path       = sourcesDir.resolve(sc.className.replace(".", File.separator) + sc.codeType.sourceFileExtension)
            val sourceCode = sc.sourceCode
            if (Files.notExists(path))
                Files.createDirectories(path.getParent)
            Files.writeString(path, sourceCode, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            path
        })
    }
}

object SourceCodeCompilationRequest {

    case class SourceCode(className: String, sourceCode: String, codeType: CompilerType)

    object SourceCode {

        def apply[V <: CompilationContext](context: V, blueprint: ClassBlueprint[V]): SourceCode = {
            val source = blueprint.toClassSource(context)
            SourceCode(context.classPackage + '.' + context.className, source, blueprint.compilerType)
        }
    }

    implicit class SourceCodeHelper(className: String) {

        def ~>(sourceCode: String, codeType: CompilerType): SourceCode = SourceCode(className, sourceCode, codeType)
    }

    abstract class Delegated[T](delegate: SourceCodeCompilationRequest[_]) extends SourceCodeCompilationRequest[T] {

        override val classPaths: Seq[Path] = delegate.classPaths

        override def additionalParams(cType: CompilerType): Array[String] = delegate.additionalParams(cType)

        override lazy val sourceCodesPaths: Seq[Path]         = delegate.sourceCodesPaths
        override lazy val sourcesDir      : Path              = delegate.sourcesDir
        override lazy val classDir        : Path              = delegate.classDir
        override var sourceCodes          : Seq[SourceCode]   = delegate.sourceCodes
        override      val workingDirectory: Path              = delegate.workingDirectory
        override      val compilationOrder: Seq[CompilerType] = delegate.compilationOrder

        override def compilerInput: InputStream = delegate.compilerInput

        override def compilerOutput: OutputStream = delegate.compilerOutput

        override def compilerErrOutput: OutputStream = delegate.compilerErrOutput
    }
}
