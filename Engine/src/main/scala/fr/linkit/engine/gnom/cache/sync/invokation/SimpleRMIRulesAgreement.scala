/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.contract.behavior.RMIRulesAgreement

class SimpleRMIRulesAgreement(currentID: String, ownerID: String,
                              desiredEngineReturn: String, acceptAll: Boolean,
                              accepted: Array[String], excluded: Array[String]) extends RMIRulesAgreement {
    private val currentIsOwner = currentID == ownerID

    override val acceptedEngines: Array[String] = accepted

    override val discardedEngines: Array[String] = excluded

    override def isAcceptAll: Boolean = acceptAll

    override def getDesiredEngineReturn: String = desiredEngineReturn

    override def mayCallSuper: Boolean = {
        if (acceptAll)
            !(excluded.contains(currentID) && (currentIsOwner || excluded.contains(ownerID)))
        else
            accepted.contains(currentID) && (currentIsOwner || accepted.contains(ownerID))
    }

    override val mayPerformRemoteInvocation: Boolean = {
        acceptAll || (accepted.nonEmpty && !accepted.contains(currentID))
    }

}
