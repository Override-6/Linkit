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

package fr.linkit.api.gnom.cache.sync.contract.behavior

import fr.linkit.api.gnom.cache.sync.contract.SyncLevel
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer

trait ConnectedObjectContext {

    val ownerID      : String
    val rootOwnerID  : String
    val currentID    : String
    val cacheOwnerID : String
    val classDef     : SyncClassDef
    val staticsTarget: Option[Class[_]]
    val choreographer: InvocationChoreographer
    val syncLevel    : SyncLevel

    def translate(tag: EngineTag): String

    /**
     * @return true if a is b depending on this context.
     * */
    def areEquals(a: EngineTag, b: EngineTag): Boolean = translate(a) == translate(b)

    def withSyncLevel(syncLevel: SyncLevel): ConnectedObjectContext
}
