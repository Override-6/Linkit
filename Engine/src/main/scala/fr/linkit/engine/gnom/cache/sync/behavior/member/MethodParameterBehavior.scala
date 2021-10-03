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

package fr.linkit.engine.gnom.cache.sync.behavior.member

import fr.linkit.api.gnom.cache.sync.behavior.member.method.parameter.{ParameterBehavior, ParameterModifier}
import org.jetbrains.annotations.Nullable

import java.lang.reflect.Parameter

case class MethodParameterBehavior[A](override val param: Parameter,
                                      override val isActivated: Boolean,
                                      @Nullable override val modifier: ParameterModifier[A]) extends ParameterBehavior[A] {
    override def getName: String = param.getName

}