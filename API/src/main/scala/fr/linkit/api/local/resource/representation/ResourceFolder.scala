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

package fr.linkit.api.local.resource.representation

import fr.linkit.api.local.resource._
import org.jetbrains.annotations.NotNull

import scala.reflect.ClassTag

/**
 * This interface depicts a [[ResourceFile]] that contains other sub [[ResourceFile]].
 * a [[ResourceFolder]] handles a list of resources, that can be present into the folder of the current
 * computer or into a distant folder from a distant driver.
 *
 * @see [[fr.linkit.api.connection.resource.RemoteResourceRepresentation]]
 * */
trait ResourceFolder extends ResourceRepresentation {

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
     * @param localOnly make this resource only known by the current machine.
     * @return an instance of [[ResourceFile]], which is default representation of the resource file.
     * @throws ResourceAlreadyPresentException if name is already registered for the resource folder.
     * @throws IllegalResourceException If the provided name contains invalid character.
     * */
    @throws[ResourceAlreadyPresentException]("If the subPath targets a resource that is already registered.")
    @throws[IllegalResourceException]("If the provided name contains invalid character")
    def openResourceFile(name: String, localOnly: Boolean): ResourceFile

    /**
     * Opens a resource folder located under this folder's path.
     * @param name the relative path in which the resource will be stored.
     * @param localOnly make this resource only known by the current machine.
     * @return an instance of [[ResourceFolder]], which is default representation of the resource folder.
     * @throws ResourceAlreadyPresentException if name is already registered for the resource folder.
     * @throws IllegalResourceException If the provided name contains invalid character.
     * */
    @throws[ResourceAlreadyPresentException]("If the subPath targets a resource that is already registered.")
    @throws[IllegalResourceException]("If the provided name contains invalid character")
    def openResourceFolder(name: String, localOnly: Boolean, behaviorOptions: AutomaticBehaviorOption*): ResourceFolder

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
     * Retrieves an [[ResourceRepresentation]] of the name and it's representation type [[R]].
     *
     * @param name the name associated with the requested resource.
     * @tparam R the type of resource expected.
     * @throws NoSuchResourceException  if no resource was found with the provided name.
     * @throws NoSuchRepresentationException if a resource was found but haven't any attached representation of type [[R]].
     * @return the expected resource representation.
     * */
    @throws[NoSuchResourceException]("If no resource was found with the provided name")
    @throws[NoSuchRepresentationException]("If a resource was found but with another type than R.")
    @NotNull
    def get[R <: ResourceRepresentation : ClassTag](name: String): R

    /**
     * Tries to retrieve an [[ResourceRepresentation]] with the same name and kind than the ones provided.
     *
     * @param name the name associated with the requested resource.
     * @tparam R the kind of resource expected.
     * @return [[Some]] instance if the resource was found AND have the same kind, or [[None]] instead
     * */
    def find[R <: ResourceRepresentation : ClassTag](name: String): Option[R]

    /**
     * Registers a resource file.
     * Be aware that the resource MUST be already stored into the handled folder.
     * No distant resource can be registered by a distant machine.
     *
     * @param name      The name of the resource to register.
     * @param localOnly If true, the registered resource will only be accessible and known by the current machine.
     * @throws NoSuchResourceException if no resource with this name was found into the resource folder.
     * @throws IllegalResourceException If the provided name contains invalid character.
     * */
    @throws[NoSuchResourceException]("If file/folder was not physically found into the maintained folder.")
    @throws[IllegalResourceException]("If the provided name contains invalid character")
    def registerFile(name: String, localOnly: Boolean): ResourceFile


    /**
     * Registers a resource folder.
     * Be aware that the resource MUST be already stored into the handled folder.
     * No distant resource can be registered by a distant machine.
     *
     * @param name      The name of the resource to register.
     * @param localOnly If true, the registered resource will only be accessible and known by the current machine.
     * @throws NoSuchResourceException if no resource with this name was found into the resource folder.
     * @throws IllegalResourceException If the provided name contains invalid character.
     * */
    @throws[NoSuchResourceException]("If file/folder was not physically found into the maintained folder.")
    @throws[IllegalResourceException]("If the provided name contains invalid character")
    def registerFolder(name: String, localOnly: Boolean, options: AutomaticBehaviorOption*): ResourceFolder

    /**
     * Links a resource with a resource class which represent and manipulate it from the code.
     *
     * @param name    the resource name to add a resource representation.
     * @param factory the factory which will create the class who represents the resource.
     * @tparam R the type of the resource representation.
     * @throws NoSuchResourceException           if no resource with this name was found into the resource folder.
     * @throws IncompatibleResourceTypeException If the requested resource type is incompatible with the name it targets.
     *                                           For example, if an [[ResourceFolder]] type is requested, but the name targets a resource file,
     *                                           as the resource can't be handled as a folder, the implementation may throw this exception.
     */
    @throws[NoSuchResourceException]("If no file/folder was physically found into the maintained folder.")
    @throws[IncompatibleResourceTypeException]("If the requested resource type is incompatible with the resource it targets.")
    def attachRepresentation[R <: ResourceRepresentation : ClassTag](name: String, factory: ResourceRepresentationFactory[R]): Unit

    /**
     * Performs a non-recursive scan of all the content of this folder.
     * Each times the scan hits a resource that is not yet registered, the scanAction gets called.
     * scanAction may determine whether the hit resource must be registered or not, attached by
     * any representation kind, or destroyed...
     *
     * The implementation can perform default operations before or after invoking the scanAction.
     *
     * @param scanAction the action to perform on each new resource.
     * */
    def scan(scanAction: String => Unit): Unit

}
