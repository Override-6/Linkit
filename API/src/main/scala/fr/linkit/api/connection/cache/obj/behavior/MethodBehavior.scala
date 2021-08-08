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
import fr.linkit.api.connection.cache.obj.invokation.{MethodInvocationHandler, WrapperMethodInvocation}
import fr.linkit.api.local.concurrency.Procrastinator
import org.jetbrains.annotations.Nullable

case class MethodBehavior(desc: MethodDescription,
                          synchronizedParams: Seq[Boolean],
                          syncReturnValue: Boolean,
                          isHidden: Boolean,
                          private val invocationRules: Array[RemoteInvocationRule],
                          @Nullable procrastinator: Procrastinator,
                          handler: MethodInvocationHandler) {

    def isRMIEnabled: Boolean = {
        handler != null
    }

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

    def completeAgreement(agreement: RMIRulesAgreementBuilder, invocation: WrapperMethodInvocation[_]): Unit = {
        invocationRules.foreach(_.apply(agreement, invocation))
    }

}

object MethodBehavior {

    /*Integer.TYPE,
            l.Byte.TYPE,
            l.Short.TYPE,
            l.Long.TYPE,
            l.Double.TYPE,
            l.Float.TYPE,
            l.Boolean.TYPE,
            Character.TYPE
     */
}