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

package fr.`override`.linkit.core.local.system.event.`extension`

abstract class ExtensionEventListener extends EventListener {

    def onExtensionsLoad(event: ExtensionsStateEvent): Unit = ()

    def onExtensionsEnable(event: ExtensionsStateEvent): Unit = ()

    def onExtensionsDisable(event: ExtensionsStateEvent): Unit = ()

    def onExtensionsStateChange(event: ExtensionsStateEvent): Unit = ()

    def onFragmentEnabled(event: FragmentEvent): Unit = ()

    def onFragmentDestroyed(event: FragmentEvent): Unit = ()

    def onRemoteFragmentEnable(event: RemoteFragmentEvent): Unit = ()

    def onRemoteFragmentDestroy(event: RemoteFragmentEvent): Unit = ()

    def onLoaderPhaseChange(event: LoaderPhaseChangeEvent): Unit = ()

    def onPropertyChange(event: RelayPropertyChangeEvent): Unit = ()

}
