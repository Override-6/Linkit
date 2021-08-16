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

package fr.linkit.api.connection.cache.obj.behavior

import fr.linkit.api.connection.cache.obj.description.MethodDescription
import fr.linkit.api.connection.cache.obj.invokation.remote.{RemoteMethodInvocationHandler, SynchronizedMethodInvocation}
import fr.linkit.api.local.concurrency.Procrastinator
import org.jetbrains.annotations.Nullable

/**
 * Determines the behaviors of a method invocation (local or remote).
 * @param desc the method description
 * @param synchronizedParams a Seq of boolean representing the method's arguments where the arguments at index n is synchronized if synchronizedParams(n) == true.
 * @param syncReturnValue if true, synchronize the return value of the method.
 * @param isHidden If hidden, this method should not be called from distant engines.
 * @param invocationRules //TODO doc
 * @param procrastinator if not null, the procrastinator that will process the method call
 * @param handler the used [[RemoteMethodInvocationHandler]]
 */
case class MethodBehavior(desc: MethodDescription,
                          synchronizedParams: Seq[Boolean],
                          syncReturnValue: Boolean,
                          isHidden: Boolean,
                          private val invocationRules: Array[RemoteInvocationRule],
                          @Nullable procrastinator: Procrastinator,
                          @Nullable handler: RemoteMethodInvocationHandler) {

    /**
     * @return true if the method can perform an RMI
     * */
    val isRMIEnabled: Boolean = {
        handler != null
    }

    /**
     * The default return value of the method, based on
     * [[https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html the default primitives value]].
     * */
    lazy val defaultReturnValue: Any = {
        val clazz = desc.javaMethod.getReturnType
        if (clazz.isPrimitive) {
            import java.{lang => l}
            clazz match {
                case l.Character.TYPE => '\u0000'
                case l.Boolean.TYPE   => false
                case _                => 0
            }
        } else {
            null
        }
    }

    def completeAgreement(agreement: RMIRulesAgreementBuilder): Unit = {
        invocationRules.foreach(_.apply(agreement))
    }

}