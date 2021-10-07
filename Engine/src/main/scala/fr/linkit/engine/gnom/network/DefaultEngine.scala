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

package fr.linkit.engine.gnom.network

import fr.linkit.api.gnom.cache.SharedCacheManager
import fr.linkit.api.gnom.cache.sync.behavior.annotation.{BasicInvocationRule, MethodControl}
import fr.linkit.api.gnom.network._
import fr.linkit.api.internal.system.Versions
import fr.linkit.engine.internal.system.StaticVersions

import java.sql.Timestamp

class DefaultEngine(override val identifier: String,
                    override val cache: SharedCacheManager) extends Engine {

    override val network       : Network        = cache.network
    override val staticAccessor: StaticAccessor = null

    //val reference: EngineReference = new EngineReference(identifier)
    override val versions: Versions = StaticVersions.currentVersions

    override val connectionDate: Timestamp = new Timestamp(System.currentTimeMillis())

    private var connectionState: ExternalConnectionState = ExternalConnectionState.CONNECTED

    @MethodControl(BasicInvocationRule.BROADCAST_IF_ROOT_OWNER) //Root owner is the Network object owner, which is the server.
    def updateState(state: ExternalConnectionState): Unit = connectionState = state

    @MethodControl(BasicInvocationRule.ONLY_OWNER)
    override def isRootReferenceSet(refId: Int): Boolean = {
        findRootReference(refId).isDefined
    }

    @MethodControl(BasicInvocationRule.ONLY_OWNER)
    override def findRootReferenceType(refId: Int): Option[Class[_]] = {
        findRootReference[AnyRef](refId).map(_.getClass)
    }

    @MethodControl(BasicInvocationRule.ONLY_OWNER)
    override def findRootReference[T](refId: Int): Option[T] = {
        ??? //rootRefStore.getReferenced(refId).asInstanceOf[Option[T]]
    }

    override def getConnectionState: ExternalConnectionState = connectionState

    override def update(): this.type = {
        cache.update()
        this
    }
}
