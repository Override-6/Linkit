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

package fr.linkit.engine.application.resource.external

import fr.linkit.api.application.resource.exception.{IncompatibleResourceTypeException, NoSuchRepresentationException}
import fr.linkit.api.application.resource.external.{Resource, ResourceEntry, ResourceFile, ResourceFolder}
import fr.linkit.api.application.resource.representation.{FileRepresentation, FolderRepresentation, ResourceRepresentation, ResourceRepresentationFactory}
import org.jetbrains.annotations.NotNull

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class DefaultResourceEntry[E <: Resource](val resource: E) extends ResourceEntry[E] {

    private val representations  = mutable.Map.empty[Class[_], ResourceRepresentation]
    private val adapter          = resource.getAdapter
    @volatile private var closed = false

    override def getResource: Resource = resource

    override def name: String = resource.name

    override def attachRepresentation[R <: ResourceRepresentation : ClassTag](implicit factory: ResourceRepresentationFactory[R, E]): R = {
        ensureAlive()

        def abort(requested: String, found: String): Unit = {
            throw IncompatibleResourceTypeException(s"Attempted to attach a $requested resource representation to a $found.")
        }

        val rClass = classTag[R].runtimeClass
        resource match {
            case _: ResourceFile   =>
                if (!classOf[FileRepresentation].isAssignableFrom(rClass))
                    abort("folder", "file")
            case _: ResourceFolder =>
                if (!classOf[FolderRepresentation].isAssignableFrom(rClass))
                    abort("file", "folder")
        }

        val representation = factory(resource)
        representations.put(classTag[R].runtimeClass, representation)
        representation
    }

    override def findRepresentation[R <: ResourceRepresentation : ClassTag]: Option[R] = {
        ensureAlive()

        representations.get(classTag[R].runtimeClass) match {
            case opt: Some[R] => opt
            case _            => resource match {
                case r: R => Some(r)
                case _    => None
            }
        }
    }

    @throws[NoSuchRepresentationException]("If a resource was found but with another type than R.")
    @NotNull
    override def getRepresentation[R <: ResourceRepresentation : ClassTag]: R = {
        ensureAlive()

        findRepresentation[R].getOrElse {
            throw NoSuchRepresentationException(s"No resource representation '${classTag[R].runtimeClass.getSimpleName}' was registered for resource ${resource.getLocation}")
        }
    }

    override def close(): Unit = {
        ensureAlive()
        representations.values.foreach(_.close())
        representations.clear()
        closed = true
    }

    def path: String = adapter.getAbsolutePath

    private def ensureAlive(): Unit = {
        if (closed)
            throw new IllegalStateException("This resource entry is closed !")
    }

}
