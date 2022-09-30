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

package fr.linkit.engine.gnom.cache.sync.generation

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.generation.GeneratedClassLoader
import fr.linkit.api.internal.compilation.CompilationRequest
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncObjectDescription
import fr.linkit.engine.gnom.cache.sync.generation.SyncClassCompilationRequestFactory.getClass
import fr.linkit.engine.internal.compilation.RuntimeClassOperations
import fr.linkit.engine.internal.compilation.factories.ClassCompilationRequestFactory
import fr.linkit.engine.internal.mapping.ClassMappings

import java.io.File
import java.nio.file.{Files, NoSuchFileException}

object SyncClassCompilationRequestFactory
        extends ClassCompilationRequestFactory[SyncObjectDescription[_], SynchronizedObject[_]](new SyncClassBlueprint(getClass.getResourceAsStream("/generation/sync_object.scbp"))) {

    override def loadClass(req: CompilationRequest[_ <: SynchronizedObject[_]],
                           context: SyncObjectDescription[_],
                           className: String,
                           loader: GeneratedClassLoader): Class[_] = {
        val syncClassFile         = req.classDir.resolve(className.replace(".", File.separator) + ".class")
        if (Files.notExists(syncClassFile))
            throw new NoSuchFileException(s"Class file for class $className at ${req.classDir} not found.")
    
        AppLoggers.Compilation.trace("Performing post compilation modifications in the class file...")
        val (byteCode, syncClass) = new SyncClassRectifier(context, className, loader, context.specs).rectifiedClass
        Files.write(syncClassFile, byteCode)
        RuntimeClassOperations.prepareClass(syncClass)
        ClassMappings.putClass(syncClass)
        syncClass
    }
}