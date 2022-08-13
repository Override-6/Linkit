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

import fr.linkit.api.gnom.cache.sync.invocation.remote.Puppeteer
import fr.linkit.api.gnom.cache.sync.tree.ObjectSyncNode

/**
 * This interface depicts a synchronized object. <br>
 * SynchronizedObject classes are dynamically generated and extends the class [[A]] <br>
 * Handwritten classes may not implement this interface.
 *
 * @see fr.linkit.api.gnom.cache.obj.generation.SyncClassCenter
 * @see SyncInstanceInstantiator
 */
trait SynchronizedObject[A <: AnyRef] extends ChippedObject[A] {

    override final def connected: A = this.asInstanceOf[A]


    /**
     * @return The used [[Puppeteer]] of this object.
     * @see Puppeteer
     */
    def getPuppeteer: Puppeteer[A]

    /**
     * this object's node.
     * */
    override def getNode: ObjectSyncNode[A]

    /**
     * @return true if this object is a distant object that is mirroring on current engine a distant implementation
     * */
    def isMirroring: Boolean

    /**
     * @return the original type of the synchronized object
     */
    def getSourceClass: Class[A]

}
