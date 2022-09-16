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

package fr.linkit.engine.gnom.cache.sync.generation.sync

import fr.linkit.api.application.resource.local.{LocalFolder, ResourceFactory, ResourceFolder}
import fr.linkit.api.application.resource.representation.ResourceRepresentationFactory
import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.engine.gnom.cache.sync.generation.sync.SyncClassStorageResource.{GeneratedClassesPackage, SyncSuffixName}
import fr.linkit.engine.internal.generation.compilation.resource.CachedClassFolderResource

class SyncClassStorageResource(resource: ResourceFolder) extends CachedClassFolderResource[SynchronizedObject[AnyRef]](resource) {

    def findClass[S <: AnyRef](classDef: SyncClassDef): Option[Class[S with SynchronizedObject[S]]] = {
        val mainClass = classDef.mainClass
        val loader = classDef.mainClass.getClassLoader
        super.findClass(adaptClassName(mainClass.getName, classDef.id), loader)
                .asInstanceOf[Option[Class[S with SynchronizedObject[S]]]]
    }

    override def findClass[S <: AnyRef](className: String, loader: ClassLoader): Option[Class[S with SynchronizedObject[AnyRef]]] = {
        super.findClass(adaptClassName(className, className.hashCode.abs), loader)
    }

    def adaptClassName(className: String, id: Int): String = {
        GeneratedClassesPackage + className + SyncSuffixName + s"_$id"
    }
}

object SyncClassStorageResource extends ResourceRepresentationFactory[SyncClassStorageResource, ResourceFolder] {

    val SyncSuffixName          = "Sync"
    val GeneratedClassesPackage = "gen."

    override def apply(resource: ResourceFolder): SyncClassStorageResource = new SyncClassStorageResource(resource)
    
}
