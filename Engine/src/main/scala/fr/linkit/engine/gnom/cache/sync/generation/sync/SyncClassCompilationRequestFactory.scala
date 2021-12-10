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

package fr.linkit.engine.gnom.cache.sync.generation.sync

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.description.SyncStructureDescription
import fr.linkit.api.gnom.cache.sync.generation.GeneratedClassLoader
import fr.linkit.api.internal.generation.compilation.{CompilationRequest, CompilerCenter}
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.sync.generation.sync.SyncClassCompilationRequestFactory.ClassBlueprint
import fr.linkit.engine.internal.generation.compilation.RuntimeClassOperations
import fr.linkit.engine.internal.generation.compilation.factories.ClassCompilationRequestFactory
import fr.linkit.engine.internal.mapping.ClassMappings

import java.io.File
import java.nio.file.{Files, NoSuchFileException}

class SyncClassCompilationRequestFactory(center: CompilerCenter)
        extends ClassCompilationRequestFactory[SyncStructureDescription[_], SynchronizedObject[_]](ClassBlueprint) {

    override def loadClass(req: CompilationRequest[Seq[Class[_ <: SynchronizedObject[_]]]],
                           context: SyncStructureDescription[_],
                           className: String,
                           loader: GeneratedClassLoader): Class[_] = {
        val wrapperClassFile         = req.classDir.resolve(className.replace(".", File.separator) + ".class")
        if (Files.notExists(wrapperClassFile))
            throw new NoSuchFileException(s"Class file for class $className at ${req.classDir} not found.")

        AppLogger.debug("Performing post compilation modifications in the class file...")
        val (byteCode, wrapperClass) = new SyncClassRectifier(context, className, loader, context.clazz)(center).rectifiedClass
        Files.write(wrapperClassFile, byteCode)
        RuntimeClassOperations.prepareClass(wrapperClass)
        ClassMappings.putClass(wrapperClass)
        wrapperClass
    }
}

object SyncClassCompilationRequestFactory {

    private val ClassBlueprint = new SyncClassBlueprint(getClass.getResourceAsStream("/generation/sync_object.scbp"))
}
