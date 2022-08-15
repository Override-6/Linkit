/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.client

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.application.connection.{ConnectionContext, ConnectionInitialisationException, ExternalConnection}
import fr.linkit.api.internal.concurrency.workerExecution
import fr.linkit.client.config.ClientConnectionConfiguration

trait ClientApplicationContext extends ApplicationContext {

    @throws[ConnectionInitialisationException]("If something went wrong during the connection's opening")
    @workerExecution
    def openConnection(config: ClientConnectionConfiguration): ExternalConnection

    @throws[NoSuchElementException]("If no connection is found into the application's cache.")
    def unregister(connectionContext: ExternalConnection): Unit

    def listConnections: Iterable[ExternalConnection]

    def findConnection(identifier: String): Option[ExternalConnection]

    def findConnection(port: Int): Option[ExternalConnection]
}
