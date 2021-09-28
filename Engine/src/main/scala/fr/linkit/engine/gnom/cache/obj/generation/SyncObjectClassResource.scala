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

package fr.linkit.engine.gnom.cache.obj.generation

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.application.resource.external.ResourceFolder
import fr.linkit.api.application.resource.representation.ResourceRepresentationFactory
import fr.linkit.engine.internal.generation.compilation.resource.CachedClassFolderResource

class SyncObjectClassResource(resource: ResourceFolder) extends CachedClassFolderResource[SynchronizedObject[Any]](resource) {

    def findClass[S](wrappedClass: Class[_]): Option[Class[S with SynchronizedObject[S]]] = {
        findClass[S](wrappedClass.getName, wrappedClass.getClassLoader).asInstanceOf[Option[Class[S with SynchronizedObject[S]]]]
    }

    override def findClass[S](className: String, loader: ClassLoader): Option[Class[S with SynchronizedObject[Any]]] = {
        super.findClass(adaptClassName(className), loader)
    }

}

object SyncObjectClassResource extends ResourceRepresentationFactory[SyncObjectClassResource, ResourceFolder] {

    val WrapperSuffixName = "Sync"
    val WrapperPackage    = "gen."

    override def apply(resource: ResourceFolder): SyncObjectClassResource = new SyncObjectClassResource(resource)
}
