package fr.`override`.linkit.api.network

import java.sql.Timestamp

import fr.`override`.linkit.api.network.cache.SharedCacheHandler
import fr.`override`.linkit.api.system.Version

trait NetworkEntity {

    val identifier: String

    val cache: SharedCacheHandler

    val connectionDate: Timestamp

    def addOnStateUpdate(action: ConnectionState => Unit): Unit

    def getConnectionState: ConnectionState

    def getProperty(name: String): Serializable

    def setProperty(name: String, value: Serializable): Unit

    def getRemoteConsole: RemoteConsole

    def getRemoteErrConsole: RemoteConsole

    def getApiVersion: Version

    def getRelayVersion: Version

    def listRemoteFragmentControllers: List[RemoteFragmentController]

    def getFragmentController(nameIdentifier: String): Option[RemoteFragmentController]

}
