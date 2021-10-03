/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.application.plugin

import fr.linkit.api.internal.system.fsa.FileAdapter

import scala.collection.mutable.ListBuffer

class PluginClassLoaderBridge {

    private val loaders = ListBuffer.empty[PluginClassLoader]

    def newClassLoader(urls: Array[FileAdapter]): PluginClassLoader = {
        val loader = new PluginClassLoader(urls, this)
        loaders += loader
        loader
    }

    def loadClass(name: String, caller: PluginClassLoader): Class[_] = {
        for (childrenClassLoader <- loaders) {
            //Ensures that the caller will not try to load the class again.
            if (childrenClassLoader != caller) try {
                return childrenClassLoader.loadClass(name)
            } catch {
                case e: ClassNotFoundException =>
                /*
                 * childrenClassLoader did not found the class: let's continue
                 * iteration over loaders in order to find the class.
                 */
            }
        }
        //Iteration terminated: none of the loaders found the class.
        throw new ClassNotFoundException("Plugin '")
    }

}
