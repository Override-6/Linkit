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

package fr.linkit.client.connection

import fr.linkit.api.gnom.network.tag.NameTag
import fr.linkit.api.gnom.persistence.ObjectTranslator
import fr.linkit.client.ClientApplication
import fr.linkit.client.config.ClientConnectionConfiguration
import fr.linkit.engine.gnom.packet.traffic.DynamicSocket

case class ClientConnectionSession(socket       : DynamicSocket,
                                   appContext   : ClientApplication,
                                   configuration: ClientConnectionConfiguration,
                                   serverNameTag: NameTag,
                                   translator   : ObjectTranslator) {

    val currentName: NameTag = NameTag(configuration.connectionName)
}

