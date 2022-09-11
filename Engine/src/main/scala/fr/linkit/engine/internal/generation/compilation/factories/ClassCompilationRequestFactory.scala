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

package fr.linkit.engine.internal.generation.compilation.factories

import fr.linkit.api.gnom.cache.sync.generation.GeneratedClassLoader
import fr.linkit.api.internal.language.cbp.ClassBlueprint
import fr.linkit.api.internal.generation.compilation.access.CompilerType
import fr.linkit.api.internal.generation.compilation.{CompilationContext, CompilationRequest, CompilationResult}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.internal.generation.compilation.SourceCodeCompilationRequest.SourceCode
import fr.linkit.engine.internal.generation.compilation.{AbstractCompilationRequestFactory, AbstractCompilationResult, SourceCodeCompilationRequest}

import java.nio.file.Path
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.internal.generation.compilation.access.CommonCompilerType
import fr.linkit.engine.internal.mapping.ClassMappings

class ClassCompilationRequestFactory[I <: CompilationContext, C](blueprint: ClassBlueprint[I]) extends AbstractCompilationRequestFactory[I, Class[_ <: C]] {


    override def createMultiRequest(contexts: Seq[I], workingDir: Path): SourceCodeCompilationRequest[Seq[Class[_ <: C]]] = {
        new SourceCodeCompilationRequest[Seq[Class[_ <: C]]] { req =>

            override val workingDirectory: Path              = workingDir
            override val classPaths      : Seq[Path]         = defaultClassPaths :+ classDir
            override val compilationOrder: Seq[CompilerType] = Seq(blueprint.compilerType)
            override var sourceCodes     : Seq[SourceCode]   = contexts.map(SourceCode(_, blueprint))

            override def conclude(outs: Seq[Path], compilationTime: Long): CompilationResult[Seq[Class[_ <: C]]] = {
                new AbstractCompilationResult[Seq[Class[_ <: C]]](outs, compilationTime, req) {
                    lazy val result: Option[Seq[Class[_ <: C]]] = {
                        Some(contexts
                                .map { context =>
                                    val syncClassName = context.classPackage + '.' + context.className
                                    val loader           = new GeneratedClassLoader(req.classDir, context.parentLoader, Seq(classOf[LinkitApplication].getClassLoader))
                                    loadClass(req, context, syncClassName, loader).asInstanceOf[Class[_ <: C]]
                                })
                    }

                    override def getValue: Option[Seq[Class[_ <: C]]] = {
                        result
                    }
                }
            }
        }
    }

    def loadClass(req: CompilationRequest[Seq[Class[_ <: C]]], context: I, className: String, loader: GeneratedClassLoader): Class[_] = {
        val clazz = loader.loadClass(className)
        ClassMappings.putClass(clazz)
        clazz
    }
}