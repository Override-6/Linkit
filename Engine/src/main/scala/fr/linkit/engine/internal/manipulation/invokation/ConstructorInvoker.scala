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

package fr.linkit.engine.internal.manipulation.invokation

import fr.linkit.engine.gnom.cache.sync.generation.sync.SyncClassRectifier
import fr.linkit.engine.internal.manipulation.ClassTypeTranslator

import java.lang.reflect.Constructor

class ConstructorInvoker(constructor: Constructor[_]) extends Invoker[Unit] {

    private final val params     = constructor.getParameterTypes
    private final val signature  = SyncClassRectifier.getMethodDescriptor(params, Void.TYPE)
    private final val paramTypes = params.map(ClassTypeTranslator.determineType)
    private final val returnType = ClassTypeTranslator.determineType(Void.TYPE)

    override def invoke(target: Any, args: Array[Any]): Unit = {
        ObjectInvocator.invokeMethod0(target, "<init>", signature, paramTypes, returnType, args.asInstanceOf[Array[AnyRef]])
    }
}