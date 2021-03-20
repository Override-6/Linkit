package fr.`override`.linkit.api.connection.network

import fr.`override`.linkit.api.connection.network.cache.SharedCacheManager

import java.sql.Timestamp

trait NetworkEntity extends Updatable {

    val identifier: String

    val cache: SharedCacheManager

    val network: Network

    def connectionDate: Timestamp

    //def apiVersion: Version

    //def relayVersion: Version

    def getConnectionState: ConnectionState

    //def getRemoteConsole: RemoteConsole

    //def getRemoteErrConsole: RemoteConsole

    //def listRemoteFragmentControllers: List[RemoteFragmentController]

    //def getFragmentController(nameIdentifier: String): Option[RemoteFragmentController]

    def toString: String

}
