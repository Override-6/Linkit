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

package fr.linkit.api.connection.cache.repo

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class PuppetDescriptions {

    private val descriptions = new mutable.HashMap[Class[_], PuppetDescription[_]]

    def getDescription[B: ClassTag]: PuppetDescription[B] = {
        val rClass = classTag[B].runtimeClass.asInstanceOf[Class[B]]
        descriptions.getOrElseUpdate(rClass, new PuppetDescription[B](rClass))
                .asInstanceOf[PuppetDescription[B]]
    }
}

object PuppetDescriptions {

    private val default = new PuppetDescriptions()

    def getDefault: PuppetDescriptions = default
}
