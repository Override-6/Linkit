/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.core.connection.resource

import fr.linkit.api.connection.ConnectionContext
import fr.linkit.api.connection.resource.RemoteResourceFolder
import fr.linkit.api.local.resource.ResourceListener
import fr.linkit.api.local.resource.external.ResourceFolder
import fr.linkit.api.local.system.fsa.FileAdapter
import fr.linkit.core.local.resource.base.BaseResourceFolder

class SimpleRemoteResourceFolder(adapter: FileAdapter,
                                 listener: ResourceListener,
                                 parent: ResourceFolder,
                                 owner: ConnectionContext)
        extends BaseResourceFolder(parent, listener, adapter) with RemoteResourceFolder {

    override def getConnectionOwner: ConnectionContext = owner
}
