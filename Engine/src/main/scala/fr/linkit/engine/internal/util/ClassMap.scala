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

package fr.linkit.engine.internal.util

import scala.collection.mutable

class ClassMap[V] extends mutable.HashMap[Class[_], V]() {
    
    def this(other: ClassMap[V]) {
        this()
        addAll(other.iterator)
    }
    
    def this(other: Map[Class[_], V]) {
        this()
        addAll(other.iterator)
    }
    
    override def apply(key: Class[_]): V = get(key).get
    
    override def get(key: Class[_]): Option[V] = {
        val opt = super.get(key)
        if (opt.isDefined)
            opt
        else getFirstSuper(key)
    }
    
    private def getFirstSuper(key: Class[_]): Option[V] = {
        if (key == null)
            return super.get(classOf[Object])
            
        val interfaces = key.getInterfaces
        for (interface <- interfaces) {
            val opt = super.get(interface)
            if (opt.isDefined)
                return opt
        }
        for (interface <- interfaces) {
            val opt = get(interface)
            if (opt.isDefined)
                return opt
        }
        get(key.getSuperclass)
    }
    
}
