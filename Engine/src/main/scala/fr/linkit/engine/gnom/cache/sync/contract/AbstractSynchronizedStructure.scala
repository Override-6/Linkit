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

package fr.linkit.engine.gnom.cache.sync.contract

import fr.linkit.api.gnom.cache.sync.contract.SynchronizedStructure

abstract class AbstractSynchronizedStructure[M, F] protected() extends SynchronizedStructure[M, F] {

    protected val methods: Map[Int, M]

    protected val fields: Map[Int, F]

    override def listMethods(): Iterable[M] = {
        methods.values
    }

    override def listField(): Iterable[F] = {
        fields.values
    }

    override def getMethod(id: Int): Option[M] = methods.get(id)

    override def getField(id: Int): Option[F] = fields.get(id)

}
