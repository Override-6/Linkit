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

package fr.linkit.api.gnom.cache.sync

import fr.linkit.api.gnom.cache.SharedCache
import fr.linkit.api.gnom.cache.sync.behavior.ObjectBehaviorStore
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceGetter
import fr.linkit.api.gnom.cache.sync.tree.SynchronizedObjectTreeStore
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.packet.PacketAttributesPresence

/**
 * The main class of the Synchronized object system.<br>
 * This classes is used as a [[SharedCache]] (see [[fr.linkit.api.gnom.cache.SharedCacheManager]] for more about shared caches.<br>
 * With this cache, you can post or retrieve an object of type [[A]]. <br>
 * Once the object is posted in the cache, it's cloned then transformed to an object of type `A with SynchronizedObject[A]`.<br>
 * Then, The object is broadcasted to all engines that are attached to this cache. <bt>
 * All method invocations performed on the transformed object will be synchronized,
 * This means that an RMI may occur following the [[fr.linkit.api.gnom.cache.sync.behavior.ObjectBehavior]] of the synchronized object.<br>
 * Notes: - A Synchronized object of type `A with SynchronizedObject[A]` can also hold inner synchronized objects of [[AnyRef]] type.
 * These inner objects can be fields, or method parameters or return values.
 *        - An object posted on the cache is called a "Root object", they must be of type [A] but, as said before, they can contains
 * other objects of any type.
 *
 * @tparam A the type of root objects.
 * */
trait SynchronizedObjectCache[A <: AnyRef] extends PacketAttributesPresence with SharedCache {

    val network: Network

    /**
     * Once an object [[A]] gets posted, it will create a [[fr.linkit.api.gnom.cache.sync.tree.SynchronizedObjectTree]],
     * in which the root node ([[fr.linkit.api.gnom.cache.sync.tree.SyncNode]]) will contains sub nodes for inner synchronized objects. <br>
     * The inner objects can be synchronized fields, synchronized method parameters and return values of the object.<br>
     * If a field, a parameter or whatever contains sub synchronized objects, other nodes will be set as their child.
     * Here is an example :
     * {{{
     *SyncNode[A] (id: 12, path: 12) :
     *           -> Field SyncNode[B] (id: 78, path: 12/78) :
     *                    -> Field SyncNode[BC]               (id: 7, path: 12/78/7)
     *                    -> Method Return Value SyncNode[BD] (id: 8, path: 12/78/8)
     *           -> Field SyncNode[C] (id: 8, path: 12/8)   :
     *                    -> Method Parameter SyncNode[CA] (id: 9, path: 12/8/9)
     *           -> Method Parameter SyncNode[E] (id: 9, path: 12/9)
     * }}}
     * Each node contains an ID, the path is an array of ids from the root's id to the node id
     *
     * @see [[fr.linkit.api.gnom.cache.sync.tree.SyncNode]]
     * @see [[fr.linkit.api.gnom.cache.sync.tree.SynchronizedObjectTree]]
     * */
    val treeCenter: SynchronizedObjectTreeStore[A]

    /**
     * The default behavior tree for an [[fr.linkit.api.gnom.cache.sync.tree.SynchronizedObjectTree]].
     * "the behavior of a tree" is simply a set of [[fr.linkit.api.gnom.cache.sync.behavior.ObjectBehavior]]
     * that will set the behavior of each objects of a tree.
     * */
    val defaultTreeViewBehavior: ObjectBehaviorStore

    /**
     * posts an object in the cache.
     * The behavior of the object and sub objects will depends on the [[defaultTreeViewBehavior]]
     *
     * @throws CanNotSynchronizeException If the given object is a synchronized object.
     *                                    (No matters if the object is handled by this cache or not)
     * @param id  the identifier of the root object
     * @param obj the object to synchronize.
     * @return the synchronized object.
     * */
    def syncObject(id: Int, creator: SyncInstanceGetter[A]): A with SynchronizedObject[A]

    /**
     * @param id       the identifier of the root object
     * @param obj      the object to synchronize.
     * @param behavior the behavior tree of the object and its inner objects
     * @return the synchronized object.
     * */
    def syncObject(id: Int, creator: SyncInstanceGetter[A], behavior: ObjectBehaviorStore): A with SynchronizedObject[A]

    /**
     * Finds a synchronized object in the cache.
     *
     * @param id the id of the root object that must be retrieved.
     * @return None if no object is posted on the given id, `Some(A with SynchronizedObject[A])` instead.
     */
    def findObject(id: Int): Option[A with SynchronizedObject[A]]

    def getOrSynchronize(id: Int)(or: => SyncInstanceGetter[A]): A = findObject(id).getOrElse(syncObject(id, or))

    /**
     * @param id the object's identifier.
     * @return true if an object of the given id is posted in this cache.
     * */
    def isRegistered(id: Int): Boolean

}
