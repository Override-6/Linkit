package fr.`override`.linkit.skull.internal.plugin

import fr.`override`.linkit.skull.internal.plugin.fragment.PluginFragment

trait Plugin {

    protected def putFragment(fragment: PluginFragment): Unit

    protected def getFragment[F <: PluginFragment](extensionClass: Class[_ <: Plugin], fragmentClass: Class[F]): Option[F]

    protected def getFragmentOrAbort[F <: PluginFragment](extensionClass: Class[_ <: Plugin], fragmentClass: Class[F]): F

    def onLoad(): Unit = ()

    def onEnable(): Unit

    def onDisable(): Unit = ()
}