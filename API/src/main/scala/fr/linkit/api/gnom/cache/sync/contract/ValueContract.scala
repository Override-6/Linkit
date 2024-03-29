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

package fr.linkit.api.gnom.cache.sync.contract

import fr.linkit.api.gnom.cache.sync.contract.level.SyncLevel

trait ValueContract {

    val kind: SyncLevel

    //if true, and if the registrationKind is for a connectable object,
    // the contracted value of the contract will automatically get chipped if the runtime
    // class of the value don't support manipulations performed by the sync class generation system.
    //have no effects if the registrationKind.isConnectable is set to false.
    val autoChip: Boolean

}

