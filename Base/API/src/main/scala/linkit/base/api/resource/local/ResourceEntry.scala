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

package linkit.base.api.resource.local

import linkit.base.api.resource.exception.{IncompatibleResourceTypeException, NoSuchRepresentationException}
import linkit.base.api.resource.representation.{ResourceRepresentation, ResourceRepresentationFactory}
import org.jetbrains.annotations.NotNull

import java.io.Closeable
import scala.reflect.ClassTag

/**
 * This class is an entry for resources that transforms an actual file/folder into
 * any resource representation.
 * Default representations of type [[Resource]] may be automatically attached.
 * */
trait ResourceEntry[+E <: Resource] extends Closeable {

    /**
     * This resource name
     * */
    def name: String

    /**
     * A representation of the folder that contains this resource.
     * */
    def getResource: Resource

    /**
     * Links a resource with a resource class and a String tag which represent it.
     *
     * @param tag the tag used to refer the representation.
     * @param factory the factory which will create a representation instance of type [[R]].
     * @tparam R the type of the resource representation.
     * @throws IncompatibleResourceTypeException If the requested resource type is incompatible with the name it targets.
     *                                           For example, if an [[ResourceFolder]] type is requested, but the name targets a resource file,
     *                                           as the resource can't be handled as a folder, the implementation may throw this exception.
     */
    @throws[IncompatibleResourceTypeException]("If the requested resource type is incompatible with the resource it targets.")
    def attachRepresentation[R <: ResourceRepresentation : ClassTag](tag: String = null)(implicit factory: ResourceRepresentationFactory[R, E]): R


    /**
     * Retrieves the wanted representation of the resource.
     *
     * @tparam R the type of resource expected.
     * @throws NoSuchRepresentationException if the resource haven't any attached representation of type [[R]].
     * @return the expected resource representation.
     * */
    @throws[NoSuchRepresentationException]("If a resource was found but with another type than R.")
    @NotNull
    def getRepresentation[R <: ResourceRepresentation : ClassTag](tag: String = null): R


    /**
     * Retrieves the wanted representation of the resource. If no resource is found, create one.
     *
     * @tparam R the type of resource expected.
     * @throws NoSuchRepresentationException if the resource haven't any attached representation of type [[R]].
     * @return the expected resource representation.
     * */
    @throws[NoSuchRepresentationException]("If a resource was found but with another type than R.")
    @NotNull
    def getOrAttachRepresentation[R <: ResourceRepresentation : ClassTag](tag: String = null)(implicit factory: ResourceRepresentationFactory[R, E]): R = {
        findRepresentation[R](tag).getOrElse(attachRepresentation[R](tag))
    }


    /**
     * Tries to Retrieves the wanted representation of the resource.
     * The representation must extends [[R]]
     *
     * @tparam R the kind of resource expected.
     * @return [[Some]] if a representation of type [[R]] was found, or [[None]] instead
     * */
    def findRepresentation[R <: ResourceRepresentation : ClassTag](tag: String = null): Option[R]
}
