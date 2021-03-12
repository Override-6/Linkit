package fr.`override`.linkit.api.system.evente.extension

import fr.`override`.linkit.api.system.evente.EventListener
import fr.`override`.linkit.api.system.evente.extension.ExtensionEvents._

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
