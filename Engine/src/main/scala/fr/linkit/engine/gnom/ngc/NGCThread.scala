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

package fr.linkit.engine.gnom.ngc

import fr.linkit.api.gnom.referencing.linker.GeneralNetworkObjectLinker

import java.lang.ref.ReferenceQueue
import scala.collection.mutable

object NGCThread extends Thread {


    private val queue      = new ReferenceQueue[DNO]()
    private val registries = mutable.HashMap.empty[GeneralNetworkObjectLinker, GNOLRegistry]


    override def run(): Unit = {
        val key = queue.remove()
        for (registry <- registries.values) {
            if (registry.informCleaned(key)) return
        }
    }

    def test: GNOLRegistry = ???

    def registerObject(gnol: GeneralNetworkObjectLinker, dno: DNO): Unit = {
        registries.getOrElseUpdate(gnol, new GNOLRegistry(gnol)).registerObject(dno)
    }



}
