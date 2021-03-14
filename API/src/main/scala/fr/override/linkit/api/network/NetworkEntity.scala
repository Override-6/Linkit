package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.network.cache.SharedCacheHandler
import fr.`override`.linkit.api.system.Version

import java.sql.Timestamp

trait NetworkEntity extends Updatable {

    val identifier: String

    val cache: SharedCacheHandler

    def connectionDate: Timestamp

    def apiVersion: Version

    def relayVersion: Version

    def getConnectionState: ConnectionState

    def getProperty(name: String): Serializable

    def setProperty(name: String, value: Serializable): Unit

    def getRemoteConsole: RemoteConsole

    def getRemoteErrConsole: RemoteConsole

    def listRemoteFragmentControllers: List[RemoteFragmentController]

    def getFragmentController(nameIdentifier: String): Option[RemoteFragmentController]

    def toString: String

}
