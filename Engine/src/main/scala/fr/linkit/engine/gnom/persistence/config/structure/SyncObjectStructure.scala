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

package fr.linkit.engine.gnom.persistence.config.structure

import fr.linkit.api.gnom.cache.sync.SyncObjectReference
import fr.linkit.api.gnom.persistence.obj.ObjectStructure

class SyncObjectStructure(objectStruct: ObjectStructure) extends ObjectStructure {
    override def isAssignable(args: Array[Class[_]], from: Int, to: Int): Boolean = {
        args.last.isAssignableFrom(classOf[SyncObjectReference]) && objectStruct.isAssignable(args, 0, args.length - 1)
    }

    override def isAssignable(args: Array[Any], from: Int, to: Int): Boolean = {
        args.last.isInstanceOf[SyncObjectReference] && objectStruct.isAssignable(args, 0, args.length - 1)
    }
}
