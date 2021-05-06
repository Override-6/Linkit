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

package fr.linkit.api.local.resource.external

import fr.linkit.api.local.resource.exception._
import fr.linkit.api.local.resource.representation.{ResourceRepresentation, ResourceRepresentationFactory}
import fr.linkit.api.local.resource.{OpenActionShortener, ResourcesMaintainer}
import org.jetbrains.annotations.NotNull

import scala.reflect.ClassTag

/**
 * This interface depicts a [[ResourceFile]] that contains other sub [[ResourceFile]].
 * a [[ResourceFolder]] handles a list of resources, that can be present into the folder of the current
 * computer or into a distant folder from a distant driver.
 *
 * @see [[fr.linkit.api.connection.resource.RemoteResourceRepresentation]]
 * */
trait ResourceFolder extends ExternalResource {

    /**
     * The resources maintainer that handles this resources folder.
     * The way that the maintainer handles its resources registration is implementation-specific.
     *
     * @see [[ResourcesMaintainer]]
     * @return The resources maintainer that handles this resources folder.
     * */
    def getMaintainer: ResourcesMaintainer

    /**
     * @return the location from the root [[ResourceFolder]]
     * */
    def getLocation: String

    /**
     * @return the parent folder of this folder or null if this resource is the root folder.
     */
    override def getParent: ResourceFolder

    /**
     * @return The sum of every checksums of this folder.
     *         The checksum only integrate registered resources.
     */
    override def getChecksum: Long

    /**
     * Opens a resource folder located under this folder's path.
     * @param name the relative path in which the resource will be stored.
     * @tparam R the kind of resource that must be opened.
     * @return an instance of [[R]], which is the resource file.
     * @throws ResourceAlreadyPresentException if name is already registered for the resource folder.
     * @throws IllegalResourceException If the provided name contains invalid character.
     * */
    @throws[ResourceAlreadyPresentException]("If the subPath targets a resource that is already registered.")
    @throws[IllegalResourceException]("If the provided name contains invalid character")
    def openResource[R <: ExternalResource : ClassTag](name: String, factory: ExternalResourceFactory[R]): R

    /**
     * No matter if a file is registered by the maintainer or not, this method
     * must return true if any file or folder with the provided name is stored into
     * the handled folder.
     *
     * @param name the file/folder name to test.
     * @return {{{true}}} if any file/folder is stored on the current machine, {{{false}}} instead.
     * */
    def isPresentOnDrive(name: String): Boolean

    /**
     * @see [[ResourcesMaintainer.isKnown()]]
     * */
    def isKnown(name: String): Boolean = getMaintainer.isKnown(name)

    /**
     * Retrieves an [[ExternalResource]] of the name and it's representation type [[R]].
     *
     * @param name the name associated with the requested resource.
     * @tparam R the type of resource expected.
     * @throws NoSuchResourceException  if no resource was found with the provided name.
     * @throws IncompatibleResourceTypeException if a resource is not the same type of [[R]].
     * @return the expected resource representation.
     * */
    @throws[NoSuchResourceException]("If no resource was found with the provided name")
    @throws[IncompatibleResourceTypeException]("If a resource was found but with another type than R.")
    @NotNull
    def get[R <: ExternalResource : ClassTag](name: String): R

    /**
     * Retrieves or creates an [[ExternalResource]] of the name and it's representation type [[R]].
     *
     * @param name the name associated with the requested resource.
     * @tparam R the type of resource expected.
     * @return the expected resource representation.
     * */
    @NotNull
    def getOrOpen[R <: ExternalResource : ClassTag](name: String)(implicit factory: ExternalResourceFactory[R]): R

    @NotNull
    def getOrOpenShort[R <: ResourceRepresentation : ClassTag](actionShortener: OpenActionShortener[R]): R = {
        actionShortener.performOpen(this)
    }

    /**
     * Tries to retrieve an [[ExternalResource]] with the same name and kind than the ones provided.
     *
     * @param name the name associated with the requested resource.
     * @tparam R the kind of resource expected.
     * @return [[Some]] instance if the resource was found AND have the same kind, or [[None]] instead
     * */
    def find[R <: ExternalResource : ClassTag](name: String): Option[R]

    /**
     * Registers a resource folder.
     * Be aware that the resource MUST be already stored into the handled folder.
     * No distant resource can be registered by a distant machine.
     *
     * @param name      The name of the resource to register.
     * @throws IllegalResourceException If the provided name contains invalid character.
     * */
    @throws[IllegalResourceException]("If the provided name contains invalid character")
    def register[R <: ExternalResource](name: String, factory: ExternalResourceFactory[R]): R

    /**
     * Unregisters a resource.
     * This method takes no effect if the provided resource's name is unknown.
     *
     * @param name the resource name to unregister.
     * */
    def unregister(name: String): Unit

}