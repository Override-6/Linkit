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

package fr.linkit.engine.internal.compilation.factories

import SourceFileCompilationRequestFactory.AbstractRequest
import fr.linkit.api.internal.compilation.{CompilationRequest, CompilationRequestFactory, CompilationResult}
import fr.linkit.api.internal.compilation.access.CompilerType
import fr.linkit.engine.internal.mapping.ClassMappings
import java.nio.file.Path

import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.internal.compilation.AbstractCompilationResult

class SourceFileCompilationRequestFactory extends CompilationRequestFactory[Path, Path] {
    override val defaultWorkingDirectory: Path = LinkitApplication.getPathProperty("compilation.working_dir")

    override def makeRequest(context: Path, workingDir: Path): CompilationRequest[Path] = {
        new AbstractRequest[Path](workingDir, Seq(context)) {
            override def conclude(outs: Seq[Path], compilationTime: Long): CompilationResult[Path] = {
                new AbstractCompilationResult[Path](outs, compilationTime, this) {
                    override def getClasses: Option[Path] = outs.headOption
                }
            }
        }
    }

    override def makeMultiRequest(contexts: Seq[Path], workingDirectory: Path): CompilationRequest[Seq[Path]] = {
        new AbstractRequest[Seq[Path]](workingDirectory, contexts) {
            override def conclude(outs: Seq[Path], compilationTime: Long): CompilationResult[Seq[Path]] = {
                new AbstractCompilationResult[Seq[Path]](outs, compilationTime, this) {
                    override def getClasses: Option[Seq[Path]] = Some(outs)
                }
            }
        }
    }


}

object SourceFileCompilationRequestFactory {

    private abstract class AbstractRequest[P](override val workingDirectory: Path, sources: Seq[Path]) extends CompilationRequest[P] {

        override def sourceCodesPaths: Seq[Path] = sources

        override val classPaths: Seq[Path] = {
            ClassMappings
                    .getClassPaths
                    .map(cp => Path.of(cp.getLocation.toURI))
        }

        override def additionalParams(cType: CompilerType): Array[String] = Array.empty

    }
}
