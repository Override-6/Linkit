/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.api.application.network

import fr.linkit.api.gnom.cache.SharedCacheManager
import fr.linkit.api.internal.system.Versions

import java.sql.Timestamp

trait Engine extends Updatable {

    val identifier: String

    val cache: SharedCacheManager

    val staticAccessor: StaticAccessor

    val versions: Versions

    val connectionDate: Timestamp

    val network: Network

    def getConnectionState: ExternalConnectionState

    def toString: String

    def isRootReferenceSet(refId: Int): Boolean

    def findRootReferenceType(refId: Int): Option[Class[_]]

    def findRootReference[T](refId: Int): Option[T]

    /*
    * connection.accessor
    * */
}
