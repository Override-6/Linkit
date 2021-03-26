package fr.`override`.linkit.core.local.plugin

import java.net.URL

import fr.`override`.linkit.api.local.system.fsa.FileAdapter

import scala.collection.mutable.ListBuffer

class PluginClassLoaderBridge {

    private val loaders = ListBuffer.empty[PluginClassLoader]

    def newClassLoader(urls: Array[FileAdapter]): PluginClassLoader = {
        val loader = new PluginClassLoader(urls, this)
        loaders += loader
        loader
    }

    def loadClass(name: String, caller: PluginClassLoader): Class[_] = {
        for (childrenClassLoader <- loaders) {
            //Ensures that the caller will not try to load the class again.
            if (childrenClassLoader != caller) try {
                return childrenClassLoader.loadClass(name)
            } catch {
                case e: ClassNotFoundException =>
                /*
                 * childrenClassLoader did not found the class: let's continue
                 * iteration over loaders in order to find the class.
                 */
            }
        }
        //Iteration terminated: none of the loaders found the class.
        throw new ClassNotFoundException("Plugin '")
    }


}
