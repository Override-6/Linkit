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

package fr.linkit.api.gnom.cache.sync

import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceCreator
import fr.linkit.api.gnom.cache.{SharedCache, SharedCacheFactory}
import fr.linkit.api.gnom.packet.PacketAttributesPresence
import fr.linkit.api.internal.system.delegate.ImplementationDelegates

import scala.reflect.ClassTag

/**
 * The main class of the Synchronized object system.<br>
 * This classes is used as a [[SharedCache]] (see [[fr.linkit.api.gnom.cache.SharedCacheManager]] for more about shared caches.<br>
 * With this cache, you can post or retrieve an object of type [[A]]. <br>
 * Once the object is posted in the cache, it's cloned then transformed to an object of type `A with SynchronizedObject[A]`.<br>
 * Then, The object is broadcasted to all engines that are attached to this cache. <bt>
 * All method invocations performed on the transformed object will be synchronized,
 * This means that an RMI may occur following the [[fr.linkit.api.gnom.cache.sync.contract.behavior.ConnectedStructureBehavior]] of the synchronized object.<br>
 * Notes: - A Synchronized object of type `A with SynchronizedObject[A]` can also hold inner synchronized objects of [[AnyRef]] type.
 * These inner objects can be fields, or method parameters or return values.
 *        - An object posted on the cache is called a "Root object", they must be of type [A] but, as said before, they can contains
 * other objects of any type.
 *
 * @tparam A the type of root objects.
 * */
trait ConnectedObjectCache[A <: AnyRef] extends SharedCache with PacketAttributesPresence {

    /**
     * The default behavior tree for an [[fr.linkit.api.gnom.cache.sync.env.ConnectedObjectTree]].
     * "the behavior of a tree" is simply a set of [[fr.linkit.api.gnom.cache.sync.contract.behavior.SynchronizedStructureBehavior]]
     * that will set the behavior of each objects of a tree.
     * */
    val contract: ContractDescriptorData


    /**
     * posts an object in the cache.
     * The behavior of the object and sub objects will depends on the [[contract]]
     *
     * @throws CannotConnectException If the given object is a synchronized object.
     *                                    (No matters if the object is handled by this cache or not)
     * @param id  the identifier of the root object
     * @param creator         the creator that will create the synchronized object.
     * @return the synchronized object.
     * */
    def syncObject(id: Int, creator: SyncInstanceCreator[_ <: A]): A with SynchronizedObject[A]

    def mirrorObject(id: Int, creator: SyncInstanceCreator[_ <: A]): A with SynchronizedObject[A]


    /**
     * Finds a synchronized object in the cache.
     *
     * @param id the id of the root object that must be retrieved.
     * @return None if no object is posted on the given id, `Some(A with SynchronizedObject[A])` instead.
     */
    def findObject(id: Int): Option[A with SynchronizedObject[A]]

    def getOrSynchronize(id: Int)(or: => SyncInstanceCreator[_ <: A]): A with SynchronizedObject[A] = findObject(id).getOrElse(syncObject(id, or))

    /**
     * @param id the object's identifier.
     * @return true if an object of the given id is posted in this cache.
     * */
    def isRegistered(id: Int): Boolean


}

object ConnectedObjectCache extends ConnectedObjectCacheFactories {
    private val delegate = ImplementationDelegates.defaultCOCFactories

    implicit override def apply[A <: AnyRef : ClassTag]: SharedCacheFactory[ConnectedObjectCache[A]] = delegate.apply

    override def apply[A <: AnyRef : ClassTag](contract: ContractDescriptorData): SharedCacheFactory[ConnectedObjectCache[A]] = delegate.apply(contract)
}
