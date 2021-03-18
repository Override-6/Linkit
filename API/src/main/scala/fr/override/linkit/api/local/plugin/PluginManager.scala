package fr.`override`.linkit.api.local.plugin

import fr.`override`.linkit.api.local.plugin.fragment.FragmentManager

trait PluginManager {

    def load(file: String): Plugin

    def load(clazz: Class[_ <: Plugin]): Plugin

    def loadAll(folder: String): Array[Plugin]

    def loadAll(classes: Class[_ <: Plugin]*): Array[Plugin]

    def countPlugins: Int

    def fragmentManager: FragmentManager

}
