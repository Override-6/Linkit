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

package fr.linkit.engine.gnom.cache.sync.instantiation

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceCreator
import fr.linkit.engine.internal.utils.ScalaUtils

//should not be used by the user.
private[sync] class MirroringInstanceCreator[A <: AnyRef](override val tpeClass: Class[A]) extends SyncInstanceCreator[A] {

    override def getInstance(syncClass: Class[A with SynchronizedObject[A]]): A with SynchronizedObject[A] = {
        ScalaUtils.allocate[A with SynchronizedObject[A]](syncClass)
    }

    override def getOrigin: Option[A] = None
}
