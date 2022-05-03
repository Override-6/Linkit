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

import fr.linkit.api.application.resource.external.ResourceFolder
import fr.linkit.api.application.resource.representation.ResourceRepresentationFactory
import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.engine.gnom.cache.sync.generation.adaptClassName
import fr.linkit.engine.internal.generation.compilation.resource.CachedClassFolderResource

class SyncObjectClassResource(resource: ResourceFolder) extends CachedClassFolderResource[SynchronizedObject[AnyRef]](resource) {

    def findClass[S <: AnyRef](classDef: SyncClassDef): Option[Class[S with SynchronizedObject[S]]] = {
        findClass[S](classDef.mainClass.getName + s"_${classDef.id}", classDef.mainClass.getClassLoader).asInstanceOf[Option[Class[S with SynchronizedObject[S]]]]
    }

    override def findClass[S <: AnyRef](className: String, loader: ClassLoader): Option[Class[S with SynchronizedObject[AnyRef]]] = {
        super.findClass(adaptClassName(className), loader)
    }

}

object SyncObjectClassResource extends ResourceRepresentationFactory[SyncObjectClassResource, ResourceFolder] {

    val SyncSuffixName          = "Sync"
    val GeneratedClassesPackage = "gen."

    override def apply(resource: ResourceFolder): SyncObjectClassResource = new SyncObjectClassResource(resource)
}
