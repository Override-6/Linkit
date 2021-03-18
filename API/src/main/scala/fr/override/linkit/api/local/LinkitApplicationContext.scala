package fr.`override`.linkit.api.local

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.local.concurrency.Procrastinator
import fr.`override`.linkit.api.local.plugin.PluginManager
import fr.`override`.linkit.api.local.system.Version
import fr.`override`.linkit.api.local.system.config.LinkitApplicationConfiguration


//TODO Recap :
//TODO Rewrite/write Doc and README of API, RelayServer and RelayPoint
//TODO Design a better event hooking system (Object EventCategories with sub parts like ConnectionListeners, PacketListeners, TaskListeners...)
//TODO Find a solution about packets that are send into a non-registered channel : if an exception is thrown, this can cause some problems, and if not, this can cause other problems. SOLUTION : Looking for "RemoteActionDescription" that can control and get some information about an action that where made over the network.
//TODO Create a PacketTree that can let the RelaySecurityManager know the content and the structure of a packet without casting it or making weird reflection stuff.
//TODO Replace all Any types by Serializable types in network.cache
object LinkitApplicationContext {
    val ApiVersion: Version = Version(name = "API", code = "0.20.0", stable = false)
}

trait LinkitApplicationContext extends Procrastinator {

    val configuration: LinkitApplicationConfiguration

    def pluginManager(): PluginManager

    def getConnection(identifier: String): ConnectionContext

}
