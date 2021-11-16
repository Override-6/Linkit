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

package fr.linkit.engine.gnom.cache.sync.generation

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.description.SyncStructureDescription
import fr.linkit.api.gnom.cache.sync.generation.GeneratedClassLoader
import fr.linkit.api.internal.generation.compilation.CompilationRequest
import fr.linkit.engine.gnom.cache.sync.generation.SyncClassCompilationRequestFactory.DefaultClassBlueprint
import fr.linkit.engine.gnom.cache.sync.generation.bp.ScalaClassBlueprint
import fr.linkit.engine.gnom.cache.sync.generation.rectifier.ClassRectifier
import fr.linkit.engine.internal.generation.compilation.RuntimeClassOperations
import fr.linkit.engine.internal.generation.compilation.factories.ClassCompilationRequestFactory
import fr.linkit.engine.internal.mapping.ClassMappings

import java.io.File
import java.nio.file.{Files, NoSuchFileException}

class SyncClassCompilationRequestFactory extends ClassCompilationRequestFactory[SyncStructureDescription[_], SynchronizedObject[_]](DefaultClassBlueprint) {

    override def loadClass(req: CompilationRequest[Seq[Class[_ <: SynchronizedObject[_]]]], context: SyncStructureDescription[_], className: String, loader: GeneratedClassLoader): Class[_] = {
        val wrapperClassFile         = req.classDir.resolve(className.replace(".", File.separator) + ".class")
        if (Files.notExists(wrapperClassFile))
            throw new NoSuchFileException(s"class file for class $className at ${req.classDir} not found.")
        val (byteCode, wrapperClass) = new ClassRectifier(context, className, loader, context.clazz).rectifiedClass
        Files.write(wrapperClassFile, byteCode)
        RuntimeClassOperations.prepareClass(wrapperClass)
        ClassMappings.putClass(wrapperClass)
        wrapperClass
    }
}

object SyncClassCompilationRequestFactory {

    private val DefaultClassBlueprint = new ScalaClassBlueprint(getClass.getResourceAsStream("/generation/sync_object.scbp"))
}
