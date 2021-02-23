package fr.`override`.linkit.api.system.event.extension

import fr.`override`.linkit.api.system.event.SimpleEventHook
import fr.`override`.linkit.api.system.event.extension.ExtensionEvents._

//noinspection TypeAnnotation
object ExtensionEventHooks {

    type L = ExtensionEventListener

    val ExtensionsLoad = SimpleEventHook[ExtensionEventListener, ExtensionsStateEvent](_.onExtensionsLoad(_))

    val ExtensionsEnable = SimpleEventHook[ExtensionEventListener, ExtensionsStateEvent](_.onExtensionsEnable(_))

    val ExtensionsDisable = SimpleEventHook[ExtensionEventListener, ExtensionsStateEvent](_.onExtensionsDisable(_))

    val ExtensionsStateChange = SimpleEventHook[ExtensionEventListener, ExtensionsStateEvent](_.onExtensionsStateChange(_))

    val FragmentEnabled = SimpleEventHook[ExtensionEventListener, FragmentEvent](_.onFragmentEnabled(_))

    val FragmentDestroyed = SimpleEventHook[ExtensionEventListener, FragmentEvent](_.onFragmentDestroyed(_))

    val RemoteFragmentEnable = SimpleEventHook[ExtensionEventListener, RemoteFragmentEvent](_.onRemoteFragmentEnable(_))

    val RemoteFragmentDestroy = SimpleEventHook[ExtensionEventListener, RemoteFragmentEvent](_.onRemoteFragmentDestroy(_))

    val LoaderPhaseChange = SimpleEventHook[ExtensionEventListener, LoaderPhaseChangeEvent](_.onLoaderPhaseChange(_))

    val PropertyChange = SimpleEventHook[ExtensionEventListener, RelayPropertyChangeEvent](_.onPropertyChange(_))

}
