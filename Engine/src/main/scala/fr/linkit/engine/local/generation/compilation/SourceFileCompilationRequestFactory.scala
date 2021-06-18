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

import fr.linkit.api.local.generation.compilation.access.CompilerType
import fr.linkit.api.local.generation.compilation.{CompilationRequest, CompilationRequestFactory, CompilationResult}
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.generation.compilation.SourceFileCompilationRequestFactory.AbstractRequest
import fr.linkit.engine.local.mapping.ClassMappings

import java.nio.file.Path

class SourceFileCompilationRequestFactory extends CompilationRequestFactory[Path, Path] {
    override val defaultWorkingDirectory: Path = LinkitApplication.getHomePathProperty("compilation.working_dir.sources")

    override def makeRequest(context: Path, workingDir: Path): CompilationRequest[Path] = {
        new AbstractRequest[Path](workingDir, Seq(context)) {
            override def conclude(outs: Seq[Path], compilationTime: Long): CompilationResult[Path] = {
                new AbstractCompilationResult[Path](outs, compilationTime, this) {
                    override def get: Option[Path] = outs.headOption
                }
            }
        }
    }

    override def makeMultiRequest(contexts: Seq[Path], workingDirectory: Path): CompilationRequest[Seq[Path]] = {
        new AbstractRequest[Seq[Path]](workingDirectory, contexts) {
            override def conclude(outs: Seq[Path], compilationTime: Long): CompilationResult[Seq[Path]] = {
                new AbstractCompilationResult[Seq[Path]](outs, compilationTime, this) {
                    override def get: Option[Seq[Path]] = Some(outs)
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
