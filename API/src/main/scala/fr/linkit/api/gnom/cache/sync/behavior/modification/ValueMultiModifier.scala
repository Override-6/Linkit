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

package fr.linkit.api.gnom.cache.sync.behavior.modification

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.invokation.local.LocalMethodInvocation
import fr.linkit.api.gnom.network.Engine

trait ValueMultiModifier[A] {

    def modifyForField(obj: A, abstractionLimit: Class[_ >: A])(containingObject: SynchronizedObject[AnyRef], causeEngine: Engine): A

    def modifyForParameter(obj: A, abstractionLimit: Class[_ >: A])(invocation: LocalMethodInvocation[_], targetEngine: Engine, kind: MethodCompModifierKind): A

    def modifyForMethodReturnValue(obj: A, abstractionLimit: Class[_ >: A])(invocation: LocalMethodInvocation[_], targetEngine: Engine, kind: MethodCompModifierKind): A

}