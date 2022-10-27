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

package fr.linkit.engine.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIDispatchAgreement
import fr.linkit.api.gnom.network.IdentifierTag

class UsageRMIDispatchAgreement(currentID: IdentifierTag, ownerID: IdentifierTag,
                                appointedEngineReturn: IdentifierTag, acceptAll: Boolean,
                                accepted: Array[IdentifierTag], excluded: Array[IdentifierTag]) extends RMIDispatchAgreement {
    private val currentIsOwner = currentID == ownerID

    override val acceptedEngines: Array[IdentifierTag] = accepted

    override val discardedEngines: Array[IdentifierTag] = excluded

    override def isAcceptAll: Boolean = acceptAll

    override def getAppointedEngineReturn: IdentifierTag = appointedEngineReturn

    override def mayCallSuper: Boolean = {
        if (acceptAll)
            !(excluded.contains(currentID) && (currentIsOwner || excluded.contains(ownerID)))
        else
            accepted.contains(currentID) && (currentIsOwner || accepted.contains(ownerID))
    }

    override val mayPerformRemoteInvocation: Boolean = {
        acceptAll || (accepted.nonEmpty && !(accepted.length == 1 && accepted.head == currentID))
    }

    override def toString: String = {
        (if (acceptAll) {
            s"accept * -> discard ${excluded.mkString("and")}"
        } else {
            s"discard * -> accept ${accepted.mkString("and")}"
        }) + s" -> appoint $appointedEngineReturn"
    }

}
