package fr.`override`.linkit.core.internal.plugin

import fr.`override`.linkit
import fr.`override`.linkit.skull.Relay
import fr.`override`.linkit.internal
import fr.`override`.linkit.internal.plugin
import fr.`override`.linkit.skull.internal.plugin.Plugin

abstract class LinkitPlugin(protected val relay: Relay) extends Plugin {

    val name: String = getClass.getSimpleName
    implicit protected val self: LinkitPlugin = this

    private val extensionLoader = relay.extensionLoader
    private val fragmentsManager = extensionLoader.fragmentHandler
    //private val fragsChannel = relay.openChannel(4, )

    protected def putFragment(fragment: plugin.fragment.ExtensionFragment): Unit = {
        fragmentsManager.putFragment(fragment)
    }

    protected def getFragment[F <: internal.plugin.fragment.ExtensionFragment](extensionClass: Class[_ <: LinkitPlugin], fragmentClass: Class[F]): Option[F] = {
        fragmentsManager.getFragment(extensionClass, fragmentClass)
    }

    protected def getFragmentOrAbort[F <: linkit.internal.plugin.fragment.ExtensionFragment](extensionClass: Class[_ <: LinkitPlugin], fragmentClass: Class[F]): F = {
        val opt = fragmentsManager.getFragment(extensionClass, fragmentClass)
        if (opt.isEmpty) {
            throw new RelayExtensionException(s"The requested fragment '${fragmentClass.getSimpleName}' was not found for extension '${extensionClass.getSimpleName}'")
        }
        opt.get
    }

}