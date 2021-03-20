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

package fr.`override`.linkit.api.system.evente.extension

import fr.`override`.linkit.api.system.evente.extension.ExtensionEvents._
import fr.`override`.linkit.api.system.evente.{EventHookCategory, SimpleEventHook}

//noinspection TypeAnnotation
class ExtensionEventHooks extends EventHookCategory {

    type L = ExtensionEventListener

    val extensionsLoad = SimpleEventHook[ExtensionEventListener, ExtensionsStateEvent](_.onExtensionsLoad(_))

    val extensionsEnable = SimpleEventHook[ExtensionEventListener, ExtensionsStateEvent](_.onExtensionsEnable(_))

    val extensionsDisable = SimpleEventHook[ExtensionEventListener, ExtensionsStateEvent](_.onExtensionsDisable(_))

    val extensionsStateChange = SimpleEventHook[ExtensionEventListener, ExtensionsStateEvent](_.onExtensionsStateChange(_))

    val fragmentEnabled = SimpleEventHook[ExtensionEventListener, FragmentEvent](_.onFragmentEnabled(_))

    val fragmentDestroyed = SimpleEventHook[ExtensionEventListener, FragmentEvent](_.onFragmentDestroyed(_))

    val remoteFragmentEnable = SimpleEventHook[ExtensionEventListener, RemoteFragmentEvent](_.onRemoteFragmentEnable(_))

    val remoteFragmentDestroy = SimpleEventHook[ExtensionEventListener, RemoteFragmentEvent](_.onRemoteFragmentDestroy(_))

    val loaderPhaseChange = SimpleEventHook[ExtensionEventListener, LoaderPhaseChangeEvent](_.onLoaderPhaseChange(_))

    val propertyChange = SimpleEventHook[ExtensionEventListener, RelayPropertyChangeEvent](_.onPropertyChange(_))

}
