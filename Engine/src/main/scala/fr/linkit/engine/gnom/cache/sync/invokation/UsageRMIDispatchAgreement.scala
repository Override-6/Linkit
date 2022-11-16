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
import fr.linkit.api.gnom.network.tag.{Current, NetworkFriendlyEngineTag, UniqueTag}

class UsageRMIDispatchAgreement private(context              : ConnectedObjectContext,
                                        appointedEngineReturn: UniqueTag with NetworkFriendlyEngineTag,
                                        val selection        : NetworkFriendlyEngineTag) extends RMIDispatchAgreement {

    private val resolver = context.resolver

    def this(context: ConnectedObjectContext, appointedEngineReturn: UniqueTag, selection: NetworkFriendlyEngineTag) {
        this(context, context.translate(appointedEngineReturn), selection)
    }

    override def getAppointedEngineReturn: UniqueTag with NetworkFriendlyEngineTag = appointedEngineReturn

    override val currentMustReturn: Boolean = {
        resolver.isEquivalent(Current, appointedEngineReturn)
    }

    override val mayCallSuper: Boolean = {
        //call super if the current engine is included in the invocation selection
        resolver.isIncluded(Current, selection)
    }

    override val mayPerformRemoteInvocation: Boolean = {
        //perform rmi is the selection include another engine that the current engine.
        //NOTE: selection cannot be empty
        !resolver.isEquivalent(selection, Current)
    }

    override def toString: String = s"RMIAgreement($selection, $appointedEngineReturn)"

}
