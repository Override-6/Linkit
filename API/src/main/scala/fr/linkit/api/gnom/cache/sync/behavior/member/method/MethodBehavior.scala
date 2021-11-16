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

package fr.linkit.api.gnom.cache.sync.behavior.member.method

import fr.linkit.api.gnom.cache.sync.behavior.member.MemberBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.method.parameter.ParameterBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.method.returnvalue.ReturnValueBehavior
import fr.linkit.api.gnom.cache.sync.description.MethodDescription
import fr.linkit.api.gnom.cache.sync.invokation.remote.MethodInvocationHandler
import fr.linkit.api.internal.concurrency.Procrastinator
import org.jetbrains.annotations.Nullable

trait MethodBehavior extends MemberBehavior {

              val desc               : MethodDescription
              val parameterBehaviors : Array[ParameterBehavior[AnyRef]]
    @Nullable val returnValueBehavior: ReturnValueBehavior[AnyRef]
              val isHidden                  : Boolean
              val forceLocalInnerInvocations: Boolean
              val defaultReturnValue        : Any
    @Nullable val procrastinator     : Procrastinator
    @Nullable val handler            : MethodInvocationHandler

}
