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

package fr.linkit.api.connection.cache.repo.description

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}
import scala.reflect.runtime.universe._
import scala.reflect.api
import scala.reflect.api.{TypeCreator, Universe}

class PuppetDescriptions {

    private val descriptions = new mutable.HashMap[Class[_], PuppetDescription[_]]

    def getDescription[B: TypeTag : ClassTag]: PuppetDescription[B] = {
        val rClass = classTag[B].runtimeClass.asInstanceOf[Class[B]]
        descriptions.getOrElseUpdate(rClass, PuppetDescription[B](rClass))
            .asInstanceOf[PuppetDescription[B]]
    }

    def getDescFromClass[B](clazz: Class[_]): PuppetDescription[B] = {
        val mirror   = runtimeMirror(clazz.getClassLoader)
        val clSymbol = mirror.staticClass(clazz.getName)
        val tpe      = clSymbol.selfType
        val tag      = TypeTag[B](mirror, new TypeCreator {
            override def apply[U <: Universe with Singleton](m: api.Mirror[U]): U#Type = {
                tpe.asInstanceOf[U#Type]
            }
        })
        descriptions.getOrElseUpdate(clazz, PuppetDescription[B](clazz)(tag))
            .asInstanceOf[PuppetDescription[B]]
    }
}

object PuppetDescriptions {

    private val default = new PuppetDescriptions()

    def getDefault: PuppetDescriptions = default
}
