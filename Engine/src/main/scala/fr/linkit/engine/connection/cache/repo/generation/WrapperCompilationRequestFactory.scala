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
import fr.linkit.api.connection.cache.repo.generation.GeneratedClassClassLoader
import fr.linkit.api.local.generation.cbp.ClassBlueprint
import fr.linkit.api.local.generation.compilation.CompilationResult
import fr.linkit.api.local.generation.compilation.access.CompilerType
import fr.linkit.engine.connection.cache.repo.generation.WrappersClassResource.{WrapperMetaPrefixName, WrapperPrefixName}
import fr.linkit.engine.local.generation.compilation.SourceCodeCompilationRequest.SourceCode
import fr.linkit.engine.local.generation.compilation.access.CommonCompilerTypes
import fr.linkit.engine.local.generation.compilation.{AbstractCompilationRequestFactory, AbstractCompilationResult, SourceCodeCompilationRequest}

import java.nio.file.Path
import scala.collection.mutable

class WrapperCompilationRequestFactory extends AbstractCompilationRequestFactory[PuppetDescription[_], Class[_]] {

    private val blueprints = new mutable.HashMap[CompilerType, ClassBlueprint[PuppetDescription[_]]]()

    def registerClassBlueprint(compilerType: CompilerType, bp: ClassBlueprint[PuppetDescription[_]]): Unit = {
        blueprints.put(compilerType, bp)
    }

    override def createMultiRequest(contexts: Seq[PuppetDescription[_]], workingDir: Path): SourceCodeCompilationRequest[Seq[Class[_]]] = {
        new SourceCodeCompilationRequest[Seq[Class[_]]] { req =>

            override val workingDirectory: Path = workingDir

            override var sourceCodes: Seq[SourceCode] = {
                contexts.flatMap(getSourceCode) //TODO put in PuppetDescription the preferred compiler type
            }

            override def conclude(outs: Seq[Path], compilationTime: Long): CompilationResult[Seq[Class[_]]] = {
                new AbstractCompilationResult[Seq[Class[_]]](outs, compilationTime, req) {
                    override def getResult: Option[Seq[Class[_]]] = {
                        Some(contexts
                                .map { desc =>
                                    val clazz            = desc.clazz
                                    val wrapperClassName = adaptClassName(clazz.getName, WrapperPrefixName)
                                    new GeneratedClassClassLoader(req.classDir, clazz.getClassLoader).loadClass(wrapperClassName)
                                })
                    }
                }
            }
        }
    }

    private def getSourceCode(desc: PuppetDescription[_]): IterableOnce[SourceCode] = {
        val scbp = blueprints(CommonCompilerTypes.Scalac)
        val jcbp = blueprints(CommonCompilerTypes.Javac)
        val name = desc.clazz.getName
        Seq(
            SourceCode(adaptClassName(name, WrapperPrefixName), scbp.toClassSource(desc), scbp.compilerType),
            SourceCode(adaptClassName(name, WrapperMetaPrefixName), jcbp.toClassSource(desc), jcbp.compilerType)
        )
    }

    {
        registerClassBlueprint(CommonCompilerTypes.Scalac, new ScalaWrapperClassBlueprint)
        registerClassBlueprint(CommonCompilerTypes.Javac, new ScalaWrapperClassBlueprint)
    }

}
