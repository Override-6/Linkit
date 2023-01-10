package linkit.base.api

import linkit.base.api.compilation.CompilerCenter
import linkit.base.api.concurrency.Procrastinator
import linkit.base.api.connection.ConnectionContext
import linkit.base.api.resource.local.ResourceFolder

trait ApplicationContext extends Procrastinator {


    val compilerCenter: CompilerCenter

    def countConnections: Int

    def getAppResources: ResourceFolder

    def shutdown(): Unit

    def isAlive: Boolean

    def listConnections: Iterable[ConnectionContext]

    def findConnection(identifier: String): Option[ConnectionContext]

    def findConnection(port: Int): Option[ConnectionContext]

}
