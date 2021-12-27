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

package fr.linkit.engine.internal.manipulation.invokation

import fr.linkit.engine.gnom.cache.sync.generation.sync.SyncClassRectifier
import fr.linkit.engine.internal.manipulation.ClassTypeTranslator

import java.lang.reflect.Method

class MethodInvoker(method: Method) extends Invoker[Any] {

    private final val name       = method.getName
    private final val signature  = SyncClassRectifier.getMethodDescriptor(method)
    private final val paramTypes = method.getParameterTypes.map(ClassTypeTranslator.determineType)
    private final val returnType = ClassTypeTranslator.determineType(method.getReturnType)

    override def invoke(target: AnyRef, args: Array[Any]): Any = {
        ObjectInvocator.invokeMethod0(target, name, signature, paramTypes, returnType, args.asInstanceOf[Array[AnyRef]])
    }

}
