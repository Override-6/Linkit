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

import fr.linkit.api.local.resource.representation.{ResourceFolder, ResourceRepresentation, ResourceRepresentationFactory}
import fr.linkit.api.local.resource.{NoSuchRepresentationException, ResourceEntry}
import org.jetbrains.annotations.NotNull

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class DefaultResourceEntry(val defaultRepresentation: ResourceRepresentation) extends ResourceEntry {

    private val representations = mutable.Map.empty[Class[_], ResourceRepresentation]
    private val adapter         = defaultRepresentation.getAdapter

    override def owner: ResourceFolder = defaultRepresentation.getParent

    override def name: String = defaultRepresentation.name

    override def attachRepresentation[R <: ResourceRepresentation : ClassTag](factory: ResourceRepresentationFactory[R]): Unit = {
        representations.put(classTag[R].runtimeClass, factory(adapter, defaultRepresentation.getParent))
    }

    override def findRepresentation[R <: ResourceRepresentation : ClassTag]: Option[R] = {
        representations.get(classTag[R].runtimeClass) match {
            case opt: Some[R] => opt
            case _            => defaultRepresentation match {
                case r: R => Some(r)
                case _    => None
            }
        }
    }

    @throws[NoSuchRepresentationException]("If a resource was found but with another type than R.")
    @NotNull
    override def getRepresentation[R <: ResourceRepresentation : ClassTag]: R = {
        findRepresentation[R].getOrElse {
            throw NoSuchRepresentationException(s"No factory of resource representation '${classTag[R].runtimeClass.getSimpleName} was registered for resource ${defaultRepresentation.getLocation}")
        }
    }

    def path: String = adapter.getAbsolutePath

}
