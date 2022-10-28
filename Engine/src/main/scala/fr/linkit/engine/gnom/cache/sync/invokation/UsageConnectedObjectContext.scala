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

package fr.linkit.engine.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.contract.behavior.ConnectedObjectContext
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.cache.sync.contract.{OwnerEngine, SyncLevel}
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.network.{Current, IdentifierTag, IllegalTagException, UniqueTag}

case class UsageConnectedObjectContext(ownerID: IdentifierTag, currentID: IdentifierTag,
                                       classDef: SyncClassDef, syncLevel: SyncLevel,
                                       choreographer: InvocationChoreographer) extends ConnectedObjectContext {


    override def translate(tag: UniqueTag): IdentifierTag = tag match {
        case id: IdentifierTag => id //nothing to translate
        case OwnerEngine       => ownerID
        case Current           => currentID
        case _                 => throw new IllegalTagException(s"Unable to translate unknown tag: $tag")
    }


    override def withSyncLevel(syncLevel: SyncLevel): ConnectedObjectContext = {
        UsageConnectedObjectContext(ownerID, classDef, syncLevel, choreographer)
    }
}
