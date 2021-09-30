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

package fr.linkit.engine.gnom.cache.obj.invokation

import fr.linkit.api.gnom.network.Engine

object ExecutorEngine {

    private val local = new ThreadLocal[Engine]()

    private var networkStarted = false

    def currentEngine: Engine = local.get()

    private[linkit] def setCurrentEngine(engine: Engine): Unit = {
        /*if (engine == null && networkStarted)
            throw new NullPointerException*/
        networkStarted = engine != null
        local.set(engine)
    }

}
