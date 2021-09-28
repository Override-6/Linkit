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

package fr.linkit.engine.internal.generation.compilation.factories

import fr.linkit.api.gnom.cache.sync.generation.GeneratedClassLoader
import fr.linkit.api.internal.language.cbp.ClassBlueprint
import fr.linkit.api.internal.generation.compilation.access.CompilerType
import fr.linkit.api.internal.generation.compilation.{CompilationContext, CompilationRequest, CompilationResult}
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.internal.LinkitApplication
import fr.linkit.engine.internal.generation.compilation.SourceCodeCompilationRequest.SourceCode
import fr.linkit.engine.internal.generation.compilation.access.CommonCompilerTypes
import fr.linkit.engine.internal.generation.compilation.{AbstractCompilationRequestFactory, AbstractCompilationResult, SourceCodeCompilationRequest}

import java.nio.file.Path

class ClassCompilationRequestFactory[I <: CompilationContext, C](blueprint: ClassBlueprint[I]) extends AbstractCompilationRequestFactory[I, Class[_ <: C]] {

    override def createMultiRequest(contexts: Seq[I], workingDir: Path): SourceCodeCompilationRequest[Seq[Class[_ <: C]]] = {
        new SourceCodeCompilationRequest[Seq[Class[_ <: C]]] { req =>

            override val workingDirectory: Path              = workingDir
            override val classPaths      : Seq[Path]         = defaultClassPaths :+ classDir
            override val compilationOrder: Seq[CompilerType] = Seq(CommonCompilerTypes.Scalac, CommonCompilerTypes.Javac)
            override var sourceCodes     : Seq[SourceCode]   = contexts.map(SourceCode(_, blueprint))

            override def conclude(outs: Seq[Path], compilationTime: Long): CompilationResult[Seq[Class[_ <: C]]] = {
                new AbstractCompilationResult[Seq[Class[_ <: C]]](outs, compilationTime, req) {
                    lazy val result: Option[Seq[Class[_ <: C]]] = {
                        Some(contexts
                                .map { context =>
                                    AppLogger.debug("Performing post compilation modifications in the class file...")
                                    val wrapperClassName = context.classPackage + '.' + context.className
                                    val loader           = new GeneratedClassLoader(req.classDir, context.parentLoader, Seq(classOf[LinkitApplication].getClassLoader))
                                    loadClass(req, context, wrapperClassName, loader).asInstanceOf[Class[_ <: C]]
                                })
                    }

                    override def getResult: Option[Seq[Class[_ <: C]]] = {
                        result
                    }
                }
            }
        }
    }

    def loadClass(req: CompilationRequest[Seq[Class[_ <: C]]], context: I, className: String, loader: GeneratedClassLoader): Class[_] = {
        loader.loadClass(className)
    }
}
