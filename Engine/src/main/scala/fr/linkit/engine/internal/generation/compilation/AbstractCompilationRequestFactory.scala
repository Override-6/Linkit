/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.generation.compilation

import fr.linkit.api.internal.generation.compilation.{CompilationRequest, CompilationRequestFactory, CompilationResult}
import java.nio.file.Path

import fr.linkit.engine.application.LinkitApplication

abstract class AbstractCompilationRequestFactory[I, O] extends CompilationRequestFactory[I, O] {

    override val defaultWorkingDirectory: Path = LinkitApplication.getPathProperty("compilation.working_dir")

    override def makeRequest(context: I, workingDirectory: Path): CompilationRequest[O] = {
        val req = createMultiRequest(Seq(context), workingDirectory)

        new SourceCodeCompilationRequest.Delegated[O](req) {
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
