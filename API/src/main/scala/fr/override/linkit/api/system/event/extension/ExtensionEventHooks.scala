package fr.`override`.linkit.api.system.event.extension

import fr.`override`.linkit.api.system.event.extension.ExtensionEvents._
import fr.`override`.linkit.api.system.event.{EventHookCategory, SimpleEventHook}

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
