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

package fr.linkit.api.gnom.network

object ExecutorEngine {

    private val local = new ThreadLocal[Engine]()

    //FIXME would cause a problem if the application uses multiple connections
    private var defaultEngine: Engine = _

    def currentEngine: Engine = {
        val engine = local.get()
        if (engine == null) {
            local.set(defaultEngine)
            return defaultEngine
        }
        engine
    }

    private[linkit] def setCurrentEngine(engine: Engine): Engine = {
        /*if (engine == null && networkStarted)
            throw new NullPointerException*/
        val last = local.get()
        local.set(engine)
        last
    }

    private[linkit] def initDefaultEngine(engine: Engine): Unit = {
        if (engine == null)
            throw new NullPointerException
        defaultEngine = engine
    }

}