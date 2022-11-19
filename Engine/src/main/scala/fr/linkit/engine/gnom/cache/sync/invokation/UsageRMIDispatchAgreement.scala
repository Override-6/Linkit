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

import fr.linkit.api.gnom.cache.sync.contract.behavior.{ConnectedObjectContext, RMIDispatchAgreement}
import fr.linkit.api.gnom.network.tag.{Current, NetworkFriendlyEngineTag, TagSelection, UniqueTag}

class UsageRMIDispatchAgreement(context              : ConnectedObjectContext,
                                appointedEngineReturn: UniqueTag with NetworkFriendlyEngineTag,
                                val selection        : TagSelection[NetworkFriendlyEngineTag]) extends RMIDispatchAgreement {

    private val resolver = context.resolver
    import resolver._

    override def getAppointedEngineReturn: UniqueTag with NetworkFriendlyEngineTag = appointedEngineReturn

    override def currentMustReturn: Boolean = Current <=> appointedEngineReturn

    override def mayCallSuper: Boolean = {
        //call super if the current engine is included in the invocation selection
        Current C selection
    }

    override def mayPerformRemoteInvocation: Boolean = {
        //perform rmi is the selection include another engine than the current engine.
        //NOTE: selection cannot be empty according to verification steps
        !(selection <=> Current)
    }

    override def toString: String = s"RMIAgreement($selection, $appointedEngineReturn)"

}
