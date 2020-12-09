package fr.overridescala.linkkit.api.`extension`

import fr.overridescala.linkkit.api.Relay
import fr.overridescala.linkkit.api.exceptions.RelayException

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class ExtensionLoaderNode(extensionInfo: ExtensionInfo, implementorExtensions: Array[ExtensionLoaderNode]) {

    private var isLoaded = false

    val weight: Int = {
        var count = implementorExtensions.length
        for (ext <- implementorExtensions)
            count = ext.weight
        count
    }

    def load(relay: Relay): Unit = {
        if (isLoaded)
            return
        relay.extensionLoader.loadExtension(extensionInfo.extensionClass)
        isLoaded = true
        implementorExtensions.foreach(_.load(relay))
    }

}

object ExtensionLoaderNode {

    def loadGraph(relay: Relay, extensionsInfo: Seq[ExtensionInfo]): Unit = {
        makeGraph(extensionsInfo).foreach(_.load(relay))
    }

    private def makeGraph(extensionsInfo: Seq[ExtensionInfo]): Array[ExtensionLoaderNode] = {
        val dependencyMaps = mutable.Map.empty[String, ListBuffer[ExtensionInfo]]
        for (ext <- extensionsInfo) {
            val dependencies = ext.dependencies

            for (dep <- dependencies) {
                if (!dependencyMaps.contains(dep)) {
                    dependencyMaps.put(dep, ListBuffer.empty)
                }
                dependencyMaps(dep) += ext
            }
        }

        val nodeCache = mutable.Map.empty[String, ExtensionLoaderNode]
        val nameCache = ListBuffer.empty[String]

        def createNode(ext: ExtensionInfo): ExtensionLoaderNode = {
            val name = ext.name
            nameCache += name
            val implementorsOpt = dependencyMaps.get(name)
            if (implementorsOpt.isEmpty) {
                if (nodeCache.contains(name))
                    return nodeCache(name)
                val node = new ExtensionLoaderNode(ext, Array())
                nodeCache.put(name, node)
                return node
            }

            val implementors = implementorsOpt.get
            val implementorsList = ListBuffer.empty[ExtensionLoaderNode]

            for (impl <- implementors) {
                if (!nameCache.contains(impl.name))
                    implementorsList += createNode(impl)
            }

            val node = new ExtensionLoaderNode(ext, implementorsList.toArray)
            nodeCache.put(ext.name, node)
            node
        }

        val roots = ListBuffer.empty[ExtensionLoaderNode]
        for (ext <- extensionsInfo) {
            if (ext.haveNoDependencies)
                roots += createNode(ext)
        }
        roots.toArray
    }


}
