/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.gnom.persistence.context.structure

import fr.linkit.engine.internal.utils.ScalaUtils

import java.lang.reflect.{Field, Modifier}
import scala.collection.mutable

class ClassObjectStructure private(objectClass: Class[_]) extends ArrayObjectStructure {

    val fields: Array[Field] = ScalaUtils.retrieveAllFields(objectClass).filterNot(f => Modifier.isTransient(f.getModifiers))
    override val types: Array[Class[_]] = fields.map(_.getType)

}
object ClassObjectStructure {

    private val cache = new mutable.HashMap[Class[_], ClassObjectStructure]()

    def apply(objectClass: Class[_]): ClassObjectStructure = {
        cache.getOrElseUpdate(objectClass, new ClassObjectStructure(objectClass))
    }
}