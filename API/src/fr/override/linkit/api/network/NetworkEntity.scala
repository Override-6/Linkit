package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.system.Version

trait NetworkEntity {

    val identifier: String

    def addOnStateUpdate(action: ConnectionState => Unit): Unit

    def getConnectionState: ConnectionState

    def getStringProperty(name: String): String

    def setStringProperty(name: String, value: String): String

    def getRemoteConsole: RemoteConsole

    def getRemoteErrConsole: RemoteConsole

    def getApiVersion: Version

    def getRelayVersion: Version

    /*def listRemoteFragmentControllers: List[RemoteFragmentController]

    def getRemoteFragmentController(nameIdentifier: String): Option[RemoteFragmentController]*/

}
