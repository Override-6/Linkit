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
import fr.linkit.api.gnom.network._
import fr.linkit.api.gnom.network.tag._

case class UsageConnectedObjectContext(ownerTag     : NameTag,
                                       classDef     : SyncClassDef,
                                       syncLevel    : SyncLevel,
                                       choreographer: InvocationChoreographer,
                                       resolver     : EngineSelector) extends ConnectedObjectContext {


    override def translate(tag: UniqueTag): NameTag = tag match {
        case name: NameTag                              => name //nothing to translate
        case OwnerEngine                                => ownerTag
        case t: UniqueTag with NetworkFriendlyEngineTag =>
            resolver.getEngine(t)
                    .map(_.nameTag)
                    .getOrElse(throw new IllegalTagException(s"Unable to translate tag '$tag'"))
        case _                                          =>
            throw new IllegalTagException(s"Unable to translate tag '$tag'")
    }


    override def deepTranslate(tag: TagSelection[EngineTag]): TagSelection[NetworkFriendlyEngineTag] = tag match {
        case Select(tag: NetworkFriendlyEngineTag) => tag
        case Select(OwnerEngine)                   => ownerTag
        case Not(tag)                              => !deepTranslate(tag)
        case Union(a, b)                           => deepTranslate(a) U deepTranslate(b)
        case Intersection(a, b)                    => deepTranslate(a) I deepTranslate(b)
        case _                                     => throw new IllegalTagException(s"Unable to translate tag '$tag'")
    }

    override def withSyncLevel(syncLevel: SyncLevel): ConnectedObjectContext = {
        UsageConnectedObjectContext(ownerTag, classDef, syncLevel, choreographer, resolver)
    }
}
