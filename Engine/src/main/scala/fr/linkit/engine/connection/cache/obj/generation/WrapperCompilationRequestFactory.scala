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

package fr.linkit.engine.connection.cache.obj.generation

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.generation.GeneratedClassLoader
import fr.linkit.api.local.generation.PuppetClassDescription
import fr.linkit.api.local.generation.cbp.ClassBlueprint
import fr.linkit.api.local.generation.compilation.CompilationResult
import fr.linkit.api.local.generation.compilation.access.CompilerType
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.obj.generation.WrapperCompilationRequestFactory.DefaultClassBlueprint
import fr.linkit.engine.connection.cache.obj.generation.bp.ScalaWrapperClassBlueprint
import fr.linkit.engine.connection.cache.obj.generation.rectifier.ClassRectifier
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.generation.compilation.SourceCodeCompilationRequest.SourceCode
import fr.linkit.engine.local.generation.compilation.access.CommonCompilerTypes
import fr.linkit.engine.local.generation.compilation.{AbstractCompilationRequestFactory, AbstractCompilationResult, SourceCodeCompilationRequest}
import fr.linkit.engine.local.mapping.ClassMappings

import java.io.File
import java.nio.file.{Files, Path}

class WrapperCompilationRequestFactory extends AbstractCompilationRequestFactory[PuppetClassDescription[_], Class[SynchronizedObject[_]]] {

    var classBlueprint: ClassBlueprint[PuppetClassDescription[_]] = DefaultClassBlueprint

    override def createMultiRequest(contexts: Seq[PuppetClassDescription[_]], workingDir: Path): SourceCodeCompilationRequest[Seq[Class[SynchronizedObject[_]]]] = {
        new SourceCodeCompilationRequest[Seq[Class[SynchronizedObject[_]]]] { req =>

            override val workingDirectory: Path              = workingDir
            override val classPaths      : Seq[Path]         = defaultClassPaths :+ classDir
            override val compilationOrder: Seq[CompilerType] = Seq(CommonCompilerTypes.Scalac, CommonCompilerTypes.Javac)
            override var sourceCodes     : Seq[SourceCode]   = {
                contexts.flatMap(getSourceCode)
            }

            override def conclude(outs: Seq[Path], compilationTime: Long): CompilationResult[Seq[Class[SynchronizedObject[_]]]] = {
                new AbstractCompilationResult[Seq[Class[SynchronizedObject[_]]]](outs, compilationTime, req) {
                    lazy val result: Option[Seq[Class[SynchronizedObject[_]]]] = {
                        Some(contexts
                                .map { desc =>
                                    AppLogger.debug("Performing post compilation modifications in the class file...")
                                    val clazz                    = desc.clazz
                                    val wrapperClassName         = adaptClassName(clazz.getName)
                                    val loader                   = new GeneratedClassLoader(req.classDir, clazz.getClassLoader, Seq(classOf[LinkitApplication].getClassLoader))
                                    AppLogger.debug("Modifications done. The class will be loaded.")
                                    val (byteCode, wrapperClass) = new ClassRectifier(desc, wrapperClassName, loader, clazz).rectifiedClass
                                    val wrapperClassFile         = req.classDir.resolve(wrapperClassName.replace(".", File.separator) + ".class")
                                    Files.write(wrapperClassFile, byteCode)
                                    SyncObjectInstantiationHelper.prepareClass(wrapperClass)
                                    ClassMappings.putClass(wrapperClass)
                                    wrapperClass
                                })
                    }

                    override def getResult: Option[Seq[Class[SynchronizedObject[_]]]] = {
                        result
                    }
                }
            }
        }
    }

    private def getSourceCode(desc: PuppetClassDescription[_]): IterableOnce[SourceCode] = {
        val name = desc.clazz.getName
        Seq(SourceCode(adaptClassName(name), classBlueprint.toClassSource(desc), classBlueprint.compilerType))
    }

}

object WrapperCompilationRequestFactory {

    private val DefaultClassBlueprint = new ScalaWrapperClassBlueprint(getClass.getResourceAsStream("/generation/puppet_wrapper_blueprint.scbp"))
}
