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

package fr.linkit.api.gnom.cache.sync.behavior.build

import fr.linkit.api.gnom.cache.sync.behavior.SynchronizedObjectType
import fr.linkit.api.gnom.cache.sync.tree.SyncNode

/**
 * The context that contains all information of how the object is currently being
 * created
 * */
trait CreationContext {

    /**
     * The parent's node of the new object we are currently defining
     * */
    def getParentNode: SyncNode[_]

    def getType: SynchronizedObjectType

}
