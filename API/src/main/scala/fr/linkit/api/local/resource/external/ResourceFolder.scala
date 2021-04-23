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

import fr.linkit.api.local.resource.ResourcesMaintainer
import fr.linkit.api.local.resource.exception._

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

    def getEntry: ResourceEntry[ResourceFolder]

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
     * @return an instance of [[ResourceFile]], which is default representation of the resource file.
     * @throws ResourceAlreadyPresentException if name is already registered for the resource folder.
     * @throws IllegalResourceException If the provided name contains invalid character.
     * */
    @throws[ResourceAlreadyPresentException]("If the subPath targets a resource that is already registered.")
    @throws[IllegalResourceException]("If the provided name contains invalid character")
    def openResourceFile(name: String): ResourceFile

    /**
     * Opens a resource folder located under this folder's path.
     * @param name the relative path in which the resource will be stored.
     * @return an instance of [[ResourceFolder]], which is default representation of the resource folder.
     * @throws ResourceAlreadyPresentException if name is already registered for the resource folder.
     * @throws IllegalResourceException If the provided name contains invalid character.
     * */
    @throws[ResourceAlreadyPresentException]("If the subPath targets a resource that is already registered.")
    @throws[IllegalResourceException]("If the provided name contains invalid character")
    def openResourceFolder(name: String): ResourceFolder

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
     * Tries to retrieve a resource folder from the provided name.
     * if a resource was found, but isn't a folder, the returned value is [[None]]
     * @param name the resource's name
     * @return Some [[ResourceFolder]] if the provided names target a known resource folder
     * */
    def findFolder(name: String): Option[ResourceFolder]

    /**
     * Tries to retrieve a resource folder from the provided name.
     * if a resource was found, but isn't a file, the returned value is [[None]]
     * @param name the resource's name
     * @return Some [[ResourceFolder]] if the provided names target a known resource file
     * */
    def findFile(name: String): Option[ResourceFile]

    /**
     * Registers a resource folder.
     * Be aware that the resource MUST be already stored into the handled folder.
     * No distant resource can be registered by a distant machine.
     *
     * @param name      The name of the resource to register.
     * @throws NoSuchResourceException if no resource with this name was found into the resource folder.
     * @throws IllegalResourceException If the provided name contains invalid character.
     * */
    @throws[NoSuchResourceException]("If file/folder was not physically found into the maintained folder.")
    @throws[IllegalResourceException]("If the provided name contains invalid character")
    def register(name: String): ExternalResource

    /**
     * Unregisters a resource.
     * This method takes no effect if the provided resource's name is unknown.
     *
     * @param name the resource name to unregister.
     * */
    def unregister(name: String): Unit

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
