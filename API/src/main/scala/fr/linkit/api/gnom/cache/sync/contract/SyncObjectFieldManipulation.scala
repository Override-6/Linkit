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

package fr.linkit.api.gnom.cache.sync.contract

import fr.linkit.api.gnom.cache.sync.ConnectedObject
import fr.linkit.api.gnom.cache.sync.contract.level.{ConcreteSyncLevel, SyncLevel}
import fr.linkit.api.gnom.network.Engine

trait SyncObjectFieldManipulation {


    def findConnectedVersion(origin: Any): Option[ConnectedObject[AnyRef]]

    def initObject(sync: ConnectedObject[AnyRef]): Unit

    def createConnectedObject(obj: AnyRef, kind: SyncLevel): ConnectedObject[AnyRef]

}
