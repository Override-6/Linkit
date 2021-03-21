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

package fr.`override`.linkit.core.local.plugin

import fr.`override`.linkit.api.local.plugin.fragment.PluginFragment
import fr.`override`.linkit.api.local.plugin.{Plugin, PluginLoadException, PluginManager}

abstract class LinkitPlugin extends Plugin {

    val name: String = getClass.getSimpleName
    implicit protected val self: LinkitPlugin = this

    private var manager: PluginManager = _
    private val fragmentsManager = manager.fragmentManager
    //private val fragsChannel = relay.openChannel(4, )

    protected def putFragment(fragment: PluginFragment): Unit = {
        fragmentsManager.putFragment(fragment)
    }

    protected def getFragment[F <: PluginFragment](extensionClass: Class[_ <: LinkitPlugin], fragmentClass: Class[F]): Option[F] = {
        fragmentsManager.getFragment(extensionClass, fragmentClass)
    }

    protected def getFragmentOrAbort[F <: PluginFragment](extensionClass: Class[_ <: LinkitPlugin], fragmentClass: Class[F]): F = {
        val opt = fragmentsManager.getFragment(extensionClass, fragmentClass)
        if (opt.isEmpty) {
            throw PluginLoadException(s"The requested fragment '${fragmentClass.getSimpleName}' was not found for extension '${extensionClass.getSimpleName}'")
        }
        opt.get
    }

    override def init(manager: PluginManager): Unit = {
        if (manager != null)
            throw new IllegalStateException("This plugin is already initialized !!")

        this.manager = manager
    }

    override def onLoad(): Unit = ()

    override def onDisable(): Unit = ()

}