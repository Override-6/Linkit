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

import fr.linkit.api.gnom.referencing.NamedIdentifier

/**
 * Used by [[fr.linkit.api.gnom.cache.sync.ConnectedObjectCache]] to store its trees
 * @tparam A the type of the tree's root object (it's the same type for SynchronizedObjectCenter[A])
 */
trait ConnectedObjectRegistry[A <: AnyRef] {


    def getUserObjectCompanion()

}
