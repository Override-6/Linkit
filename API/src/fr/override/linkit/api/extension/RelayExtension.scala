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

package fr.`override`.linkit.api.`extension`

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.`extension`.fragment.ExtensionFragment
import fr.`override`.linkit.api.exception.RelayExtensionException

abstract class RelayExtension(protected val relay: Relay) {

    val name: String = getClass.getSimpleName
    implicit protected val self: RelayExtension = this

    private val extensionLoader = relay.extensionLoader
    private val fragmentsManager = extensionLoader.fragmentHandler
    //private val fragsChannel = relay.openChannel(4, )

    def onLoad(): Unit = ()

    protected def putFragment(fragment: ExtensionFragment): Unit = {
        fragmentsManager.putFragment(fragment)
    }

    protected def getFragment[F <: ExtensionFragment](extensionClass: Class[_ <: RelayExtension], fragmentClass: Class[F]): Option[F] = {
        fragmentsManager.getFragment(extensionClass, fragmentClass)
    }

    protected def getFragmentOrAbort[F <: ExtensionFragment](extensionClass: Class[_ <: RelayExtension], fragmentClass: Class[F]): F = {
        val opt = fragmentsManager.getFragment(extensionClass, fragmentClass)
        if (opt.isEmpty) {
            throw new RelayExtensionException(s"The requested fragment '${fragmentClass.getSimpleName}' was not found for extension '${extensionClass.getSimpleName}'")
        }
        opt.get
    }

    def onEnable(): Unit

    def onDisable(): Unit = ()
}