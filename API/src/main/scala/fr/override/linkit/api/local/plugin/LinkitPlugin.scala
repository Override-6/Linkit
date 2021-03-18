package fr.`override`.linkit.api.local.plugin

import fr.`override`.linkit
import fr.`override`.linkit.api.local.plugin.{Plugin, PluginLoader}

abstract class LinkitPlugin extends Plugin {

    val name: String = getClass.getSimpleName
    implicit protected val self: LinkitPlugin = this

    private var loader: PluginLoader = _
    private val fragmentsManager = loader.fragmentHandler
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

    private[plugin] def init(loader: PluginLoader): Unit = {
        this.loader = loader
    }

    override def onLoad(): Unit = ()

    override def onDisable(): Unit = ()

}