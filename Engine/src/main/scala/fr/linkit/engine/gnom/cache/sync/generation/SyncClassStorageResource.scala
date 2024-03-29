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

import fr.linkit.api.application.resource.local.ResourceFolder
import fr.linkit.api.application.resource.representation.ResourceRepresentationFactory
import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.engine.gnom.cache.sync.contract.description.{AbstractSyncStructureDescription, SyncObjectDescription}
import fr.linkit.engine.gnom.cache.sync.generation.SyncClassStorageResource.{GeneratedClassesPackage, SyncSuffixName}
import fr.linkit.engine.internal.compilation.resource.CachedClassFolderResource

import java.nio.file.Files

class SyncClassStorageResource(resource: ResourceFolder) extends CachedClassFolderResource[SynchronizedObject[AnyRef]](resource) {

    def findClass[S <: AnyRef](classDef: SyncClassDef): Option[Class[S with SynchronizedObject[S]]] = {
        val className = SyncObjectDescription(classDef).className
        val mainClass = classDef.mainClass
        val loader    = classDef.mainClass.getClassLoader
        val genClasName = GeneratedClassesPackage + mainClass.getPackageName + '.' + className
        super.findClass(genClasName, loader)
                .asInstanceOf[Option[Class[S with SynchronizedObject[S]]]]
    }

    override def findClass[S <: AnyRef](className: String, loader: ClassLoader): Option[Class[S with SynchronizedObject[AnyRef]]] = {
        super.findClass(GeneratedClassesPackage + '.' + className, loader)
    }
}

object SyncClassStorageResource extends ResourceRepresentationFactory[SyncClassStorageResource, ResourceFolder] {

    val SyncSuffixName          = "Conn"
    val GeneratedClassesPackage = "gen."

    override def apply(resource: ResourceFolder): SyncClassStorageResource = new SyncClassStorageResource(resource)

}
