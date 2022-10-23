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

package fr.linkit.api.internal.concurrency

import fr.linkit.api.internal.system.delegate.ImplementationDelegates

import java.util.concurrent.Future


trait Procrastinator {

    def runLater[A](f: => A): Future[A]

}

object Procrastinator {

    private final val supplier = ImplementationDelegates.defaultProcrastinatorSupplier

    private[linkit] trait Supplier {
        def apply(name: String): Procrastinator

        def current: Option[Procrastinator]

        def currentWorker: Option[Worker]

    }

    def currentWorker: Option[Worker] = supplier.currentWorker

    def current: Option[Procrastinator] = supplier.current

    def apply(name: String): Procrastinator = supplier(name)
}