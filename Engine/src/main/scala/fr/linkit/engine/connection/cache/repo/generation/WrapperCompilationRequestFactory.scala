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

package fr.linkit.engine.connection.cache.repo.generation

import fr.linkit.api.connection.cache.repo.description.PuppetDescription
import fr.linkit.api.connection.cache.repo.description.PuppetDescription.MethodDescription
import fr.linkit.api.connection.cache.repo.generation.GeneratedClassClassLoader
import fr.linkit.api.local.generation.cbp.ClassBlueprint
import fr.linkit.api.local.generation.compilation.CompilationResult
import fr.linkit.api.local.generation.compilation.access.CompilerType
import fr.linkit.engine.connection.cache.repo.generation.WrapperCompilationRequestFactory.DefaultClassBlueprint
import fr.linkit.engine.connection.cache.repo.generation.WrappersClassResource.WrapperPrefixName
import fr.linkit.engine.connection.cache.repo.generation.bp.ScalaWrapperClassBlueprint
import fr.linkit.engine.connection.cache.repo.generation.rectifier.ClassRectifier
import fr.linkit.engine.local.generation.compilation.SourceCodeCompilationRequest.SourceCode
import fr.linkit.engine.local.generation.compilation.access.CommonCompilerTypes
import fr.linkit.engine.local.generation.compilation.{AbstractCompilationRequestFactory, AbstractCompilationResult, SourceCodeCompilationRequest}

import java.nio.file.Path

class WrapperCompilationRequestFactory extends AbstractCompilationRequestFactory[PuppetDescription[_], Class[_]] {

    var classBlueprint      : ClassBlueprint[PuppetDescription[_]] = DefaultClassBlueprint

    override def createMultiRequest(contexts: Seq[PuppetDescription[_]], workingDir: Path): SourceCodeCompilationRequest[Seq[Class[_]]] = {
        new SourceCodeCompilationRequest[Seq[Class[_]]] { req =>

            override val workingDirectory: Path              = workingDir
            override val classPaths      : Seq[Path]         = defaultClassPaths :+ classDir
            override val compilationOrder: Seq[CompilerType] = Seq(CommonCompilerTypes.Scalac, CommonCompilerTypes.Javac)
            override var sourceCodes     : Seq[SourceCode]   = {
                contexts.flatMap(getSourceCode) //TODO put in PuppetDescription the preferred compiler type
            }

            override def conclude(outs: Seq[Path], compilationTime: Long): CompilationResult[Seq[Class[_]]] = {
                new AbstractCompilationResult[Seq[Class[_]]](outs, compilationTime, req) {
                    override def getResult: Option[Seq[Class[_]]] = {
                            Some(contexts
                                    .map { desc =>
                                        val clazz            = desc.clazz
                                        val wrapperClassName = adaptClassName(clazz.getName, WrapperPrefixName)
                                        val loader           = new GeneratedClassClassLoader(req.classDir, clazz.getClassLoader)
                                        new ClassRectifier(desc, wrapperClassName, loader, clazz).rectifiedClass
                                    })
                    }
                }
            }
        }
    }

    private def getSourceCode(desc: PuppetDescription[_]): IterableOnce[SourceCode] = {
        val name = desc.clazz.getName
        Seq(SourceCode(adaptClassName(name, WrapperPrefixName), classBlueprint.toClassSource(desc), classBlueprint.compilerType))
    }

}

object WrapperCompilationRequestFactory {

    private val DefaultClassBlueprint       = new ScalaWrapperClassBlueprint(getClass.getResourceAsStream("/generation/puppet_wrapper_blueprint.scbp"))
}
