package fr.`override`.linkit.api.system.event.extension

import fr.`override`.linkit.api.system.event.extension.ExtensionEvents._
import fr.`override`.linkit.api.system.event.{EventHook, SimpleEventHook}

object ExtensionEventHooks {

    val ExtensionsLoad: EventHook[ExtensionsStateEvent] = SimpleEventHook()

    val ExtensionsEnable: EventHook[ExtensionsStateEvent] = SimpleEventHook()

    val ExtensionsDisable: EventHook[ExtensionsStateEvent] = SimpleEventHook()

    val ExtensionsStateChange: EventHook[ExtensionsStateEvent] = SimpleEventHook()

    val FragmentEnabled: EventHook[FragmentEvent] = SimpleEventHook()

    val FragmentDestroyed: EventHook[FragmentEvent] = SimpleEventHook()

    val RemoteFragmentEnable: EventHook[RemoteFragmentEvent] = SimpleEventHook()

    val RemoteFragmentDestroy: EventHook[RemoteFragmentEvent] = SimpleEventHook()

    val LoaderPhaseChange: EventHook[LoaderPhaseChangeEvent] = SimpleEventHook()

    val PropertyChange: EventHook[RelayPropertyChangeEvent] = SimpleEventHook()

}
