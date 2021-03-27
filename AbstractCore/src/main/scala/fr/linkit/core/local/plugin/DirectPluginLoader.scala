/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.core.local.plugin

import fr.linkit.api.local.ApplicationContext
import fr.linkit.api.local.plugin.{Plugin, PluginLoadException, PluginLoader}

import scala.util.control.NonFatal

class DirectPluginLoader(context: ApplicationContext, classes: Array[Class[_ <: Plugin]]) extends PluginLoader {

    private var count = 0

    override def nextPlugin(): Plugin = {
        if (!haveNextPlugin)
            throw new NoSuchElementException

        val clazz = classes(count)
        count += 1
        try {
            val plugin = clazz.getConstructor().newInstance()
            plugin.init(context)
            plugin
        } catch {
            case _: NoSuchMethodException =>
                throw PluginLoadException(s"Could not load '${clazz.getSimpleName}' : empty constructor is missing !")
            case NonFatal(e) =>
                throw PluginLoadException(s"Could not load '${clazz.getSimpleName}' : ${e.getMessage}", e)
            case e: Throwable =>
                throw e
        }
    }

    override def currentIndex: Int = count

    override def length: Int = classes.length

    override def haveNextPlugin: Boolean = count < length

}