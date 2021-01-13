package fr.`override`.linkit.api.`extension`

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.`extension`.fragment.ExtensionFragment
import fr.`override`.linkit.api.exception.RelayExtensionException

abstract class RelayExtension(protected val relay: Relay) {

    val name: String = getClass.getSimpleName
    implicit protected val self: RelayExtension = this

    private val extensionLoader = relay.extensionLoader
    private val fragmentsManager = extensionLoader.fragmentHandler

    def onLoad(): Unit = ()

    protected def putFragment(fragment: ExtensionFragment): Unit = {
        fragmentsManager.setFragment(fragment)
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