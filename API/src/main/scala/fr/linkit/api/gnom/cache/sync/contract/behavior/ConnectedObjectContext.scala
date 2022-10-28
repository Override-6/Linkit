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

package fr.linkit.api.gnom.cache.sync.contract.behavior

import fr.linkit.api.gnom.cache.sync.contract.SyncLevel
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.network.{EngineTag, GroupTag, IdentifierTag, UniqueTag}

trait ConnectedObjectContext {

    val currentID    : IdentifierTag
    val ownerID      : IdentifierTag
    val classDef     : SyncClassDef
    val choreographer: InvocationChoreographer
    val syncLevel    : SyncLevel

    def translate(tag: UniqueTag): IdentifierTag

    def translateAll(tags: Seq[EngineTag]): Set[IdentifierTag]


    def areEquivalent(a: EngineTag, b: EngineTag): Boolean = a match {
        case au: UniqueTag => b match {
            case bu: UniqueTag => translate(au) == translate(bu)
            case _             => false
        }
        case _: GroupTag   => a == b
    }

    def withSyncLevel(syncLevel: SyncLevel): ConnectedObjectContext
}