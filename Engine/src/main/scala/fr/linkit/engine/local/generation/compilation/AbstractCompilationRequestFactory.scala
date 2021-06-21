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

import fr.linkit.api.connection.cache.repo.description.PuppetDescription
import fr.linkit.api.local.generation.compilation.{CompilationRequest, CompilationRequestFactory, CompilationResult}
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.generation.compilation.SourceCodeCompilationRequest.SourceCode

import java.nio.file.Path

abstract class AbstractCompilationRequestFactory[I, O] extends CompilationRequestFactory[I, O] {

    override val defaultWorkingDirectory: Path = LinkitApplication.getHomePathProperty("compilation.working_dir.sources")

    override def makeRequest(context: I, workingDirectory: Path): CompilationRequest[O] = {
        val req = createMultiRequest(Seq(context), workingDirectory)

        new SourceCodeCompilationRequest[O] {
            override var sourceCodes     : Seq[SourceCode] = req.sourceCodes
            override val workingDirectory: Path            = req.workingDirectory

            override def conclude(outs: Seq[Path], compilationTime: Long): CompilationResult[O] = {
                new AbstractCompilationResult[O](outs, compilationTime, req) {
                    override def getResult: Option[O] = {
                        req.conclude(outs, compilationTime)
                                .getResult
                                .flatMap(_.headOption)
                    }
                }
            }
        }
    }

    override def makeMultiRequest(contexts: Seq[I], workingDirectory: Path): CompilationRequest[Seq[O]] = {
        createMultiRequest(contexts, workingDirectory)
    }

    protected def createMultiRequest(contexts: Seq[I], workingDir: Path): SourceCodeCompilationRequest[Seq[O]]

}
