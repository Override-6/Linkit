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

import fr.linkit.api.gnom.cache.sync.ConnectedObjectReference
import fr.linkit.api.gnom.persistence.obj.ObjectStructure

import java.lang

class SyncObjectStructure(objectStruct: ObjectStructure) extends ObjectStructure {
    
    override def isAssignable(args: Array[Class[_]], from: Int, to: Int): Boolean = {
        val last   = args.last
        val stLast = args(args.length - 2)
        last.isAssignableFrom(classOf[ConnectedObjectReference]) &&
                (stLast == lang.Boolean.TYPE) && objectStruct.isAssignable(args, 0, args.length - 2)
    }
    
    override def isAssignable(args: Array[Any], from: Int, to: Int): Boolean = {
        val last   = args.last
        val stLast = args(args.length - 2)
        last.isInstanceOf[ConnectedObjectReference] &&
                stLast.isInstanceOf[Boolean] &&
                objectStruct.isAssignable(args, 0, args.length - 2)
    }
}
