package fr.`override`.linkit.server.security

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.local.ApplicationContext
import fr.`override`.linkit.api.local.system.security.{ApplicationSecurityManager, BytesHasher}

trait ServerSecurityManager extends ApplicationSecurityManager {
    val hasher: BytesHasher
}

object ServerSecurityManager {

    class Default extends ServerSecurityManager {
        override def checkConnection(connection: ConnectionContext): Unit = ()

        override def checkApp(application: ApplicationContext): Unit = ()

        override val hasher: BytesHasher = BytesHasher.inactive
    }

    def default: ServerSecurityManager = new Default

}
