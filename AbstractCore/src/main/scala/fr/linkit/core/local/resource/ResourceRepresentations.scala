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

package fr.linkit.core.local.resource

import fr.linkit.api.local.resource.{ExternalResource, ResourceFactory}

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class ResourceRepresentations(val resource: ExternalResource) extends Serializable {

    private val representations = mutable.Map.empty[Class[_], ExternalResource]
    private val adapter         = resource.getAdapter

    def attach[R <: ExternalResource : ClassTag](factory: ResourceFactory[R]): Unit = {
        representations.put(classTag[R].runtimeClass, factory(adapter, resource.getParent))
    }

    def get[R <: ExternalResource : ClassTag]: Option[R] = {
        representations.get(classTag[R].runtimeClass) match {
            case opt: Option[R] => opt
            case _              => None
        }
    }

    def path: String = adapter.getAbsolutePath

}
