package fr.`override`.linkit.api.local.plugin.fragment

import fr.`override`.linkit.api.local.plugin.Plugin

trait FragmentManager {

    def putFragment(fragment: PluginFragment)(implicit owner: Plugin): Unit

    def getFragment[F <: PluginFragment](ownerClass: Class[_ <: Plugin], fragmentClass: Class[F]): Option[F]

}
