package fr.`override`.linkit.api.local.system.security

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.local.ApplicationContext

trait ApplicationSecurityManager {

    @throws[ConnectionSecurityException]("If the security manager rejected the initialisation.")
    def checkConnection(connection: ConnectionContext): Unit

    def checkApp(application: ApplicationContext)

}

object ApplicationSecurityManager {

    class Default extends ApplicationSecurityManager {
        override def checkConnection(connection: ConnectionContext): Unit = ()

        override def checkApp(application: ApplicationContext): Unit = ()
    }

    def default: ApplicationSecurityManager = new Default
}
