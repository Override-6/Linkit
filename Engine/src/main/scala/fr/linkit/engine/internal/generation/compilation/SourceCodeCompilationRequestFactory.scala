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

package fr.linkit.engine.internal.generation.compilation

import fr.linkit.api.gnom.cache.sync.generation.GeneratedClassLoader
import fr.linkit.api.internal.generation.compilation.CompilationResult
import fr.linkit.engine.internal.generation.compilation.SourceCodeCompilationRequest.SourceCode
import java.nio.file.Path

import fr.linkit.engine.application.LinkitApplication

object SourceCodeCompilationRequestFactory extends AbstractCompilationRequestFactory[(SourceCode, ClassLoader), Class[_]] {

    //TODO the code can be factorised (compare with AbstractCompilationRequestFactory, SourceCodeCompilationRequestFactory and WrapperCompilationRequestFactory)
    override protected def createMultiRequest(contexts: Seq[(SourceCode, ClassLoader)], workingDir: Path): SourceCodeCompilationRequest[Seq[Class[_]]] = {
        new SourceCodeCompilationRequest[Seq[Class[_]]] { request =>
            override val workingDirectory: Path            = workingDir
            override var sourceCodes     : Seq[SourceCode] = contexts.map(_._1)

            override def conclude(outs: Seq[Path], compilationTime: Long): CompilationResult[Seq[Class[_]]] = {
                new AbstractCompilationResult[Seq[Class[_]]](outs, compilationTime, request) {
                    override def getValue: Option[Seq[Class[_]]] = {
                        Some(contexts
                                .filter(context => outs.contains(workingDir.resolve(context._1.className)))
                                .map { context =>
                                    val loader = new GeneratedClassLoader(workingDir, context._2, Seq(classOf[LinkitApplication].getClassLoader))
                                    val clazz = Class.forName(context._1.className, true, loader)
                                    RuntimeClassOperations.prepareClass(clazz)
                                    clazz
                                })
                    }
                }
            }
        }
    }
}
