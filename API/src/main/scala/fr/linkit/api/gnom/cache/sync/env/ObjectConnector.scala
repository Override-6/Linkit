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

package fr.linkit.api.gnom.cache.sync.env

import fr.linkit.api.gnom.cache.sync.contract.SyncLevel
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.cache.sync.{CannotConnectException, ConnectedObject, ConnectedObjectReference}
import fr.linkit.api.gnom.network.tag.{NetworkFriendlyEngineTag, UniqueTag}
import fr.linkit.api.gnom.referencing.NamedIdentifier

import java.util.concurrent.ThreadLocalRandom

trait ObjectConnector {

    /**
     *
     * Inserts an object in the tree, transforms it into a synchronized object, and wraps the resulting object in a [[ConnectedObjectCompanion]]
     *
     * @throws CannotConnectException if the object is already synchronized.
     * @throws IllegalArgumentException   if the given parent does not belongs to this tree.
     * @throws NoSuchSyncNodeException    if the parent's path could not be found.
     * @param parent  the parent of the synchronized object that will be inserted.
     * @param id      the identifier of the created node
     * @param source  the object that will be converted to a synchronized object.
     * @param ownerID the owner of the object (Generally the engine that have created the object)
     * @tparam B the type of the object.
     * @return the created node
     */
    //TODO may not be accessible that publicly
    def insertObject[B <: AnyRef](parent       : ConnectedObjectCompanion[_],
                                  source       : AnyRef,
                                  ownerID      : UniqueTag with NetworkFriendlyEngineTag,
                                  insertionKind: SyncLevel): ConnectedObjectCompanion[B]

    /**
     *
     * Inserts an object in the tree, transforms it into a synchronized object, and wraps the resulting object in a [[ConnectedObjectCompanion]]
     *
     * @throws CannotConnectException if the object is already synchronized.
     * @throws IllegalArgumentException   if the given parent is does not belongs to this tree.
     * @throws NoSuchSyncNodeException    if the parent's path could not be found.
     * @param parentPath the parent's path of the synchronized object that will be inserted.
     * @param id         the identifier of the created node
     * @param source     the object that will be converted to a synchronized object.
     * @param ownerID    the owner of the object (Generally the engine that have created the object)
     * @tparam B the type of the object.
     * @return the created node
     */
    def insertObject[B <: AnyRef](parentPath   : Array[NamedIdentifier],
                                  source       : AnyRef,
                                  ownerID      : UniqueTag with NetworkFriendlyEngineTag,
                                  insertionKind: SyncLevel): ConnectedObjectCompanion[B] = {
        insertObject(parentPath, source, ownerID, insertionKind, ThreadLocalRandom.current().nextInt())
    }

    def insertObject[B <: AnyRef](parentPath   : Array[NamedIdentifier],
                                  source       : AnyRef,
                                  ownerID      : UniqueTag with NetworkFriendlyEngineTag,
                                  insertionKind: SyncLevel,
                                  id           : Int): ConnectedObjectCompanion[B]

    def createMirroredObject[B <: AnyRef](parentPath: Array[NamedIdentifier],
                                          classDef  : SyncClassDef,
                                          ownerID   : UniqueTag with NetworkFriendlyEngineTag,
                                          id        : Int = ThreadLocalRandom.current().nextInt()): ConnectedObjectCompanion[B]

    def createConnectedObj(parentRef: ConnectedObjectReference)(obj: Any, kind: SyncLevel): ConnectedObject[AnyRef] = {
        createConnectedObj(parentRef, ThreadLocalRandom.current().nextInt())(obj, kind)
    }

    def createConnectedObj(parentRef: ConnectedObjectReference, id: Int)(obj: Any, kind: SyncLevel): ConnectedObject[AnyRef]

}