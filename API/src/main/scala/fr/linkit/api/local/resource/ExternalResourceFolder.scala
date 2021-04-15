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

package fr.linkit.api.local.resource

import org.jetbrains.annotations.NotNull

import scala.reflect.ClassTag

/**
 * This interface depicts an [[ExternalResource]] that contains other sub [[ExternalResource]].
 * an [[ExternalResourceFolder]] handles a list of resources, that can be present into the folder of the current
 * computer or into a distant folder from a distant driver.
 *
 * @see [[fr.linkit.api.connection.resource.RemoteResource]]
 * */
trait ExternalResourceFolder extends ExternalResource {

    /**
     * The resources maintainer that handles this resources folder.
     * The way that the maintainer handles his resources registration is implementation-specific.
     * @see [[ResourcesMaintainer]]
     * @return The resources maintainer that handles this resources folder.
     * */
    def getMaintainer: ResourcesMaintainer

    /**
     * @return the absolute location in the drive that store this resources folder
     * */
    def getLocation: String

    /**
     * Retrieves an [[ExternalResource]] that have the same name and type than the ones provided.
     * @param name the name associated with the requested resource.
     * @tparam R the kind of resource expected.
     * @throws NoSuchResourceException if no resource was found with the provided name.
     * @throws BadResourceTypeException if a resource was found but with not the expected type.
     * @return the expected resource.
     * */
    @throws[NoSuchResourceException]("If no resource was found with the provided name")
    @throws[BadResourceTypeException]("If a resource was found but with another type than R.")
    @NotNull def get[R <: ExternalResource : ClassTag](name: String): R

    /**
     * Tries to retrieve an [[ExternalResource]] with the same name and kind than the ones provided.
     * @param name the name associated with the requested resource.
     * @tparam R the kind of resource expected.
     * @return [[Some]] instance if the resource was find AND have the same kind, or [[None]] instead
     * */
    def find[R <: ExternalResource : ClassTag](name: String): Option[R]
}
