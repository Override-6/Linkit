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

package fr.linkit.engine.gnom.persistence.config.structure

import fr.linkit.engine.internal.util.ScalaUtils
import java.lang.reflect.{Field, Modifier}

import scala.collection.mutable

class ClassObjectStructure private(val objectClass: Class[_]) extends ArrayObjectStructure {

    val fields: Array[Field] = ScalaUtils.retrieveAllFields(objectClass).filterNot(f => Modifier.isTransient(f.getModifiers))
    override val types: Array[Class[_]] = fields.map(_.getType)

}
object ClassObjectStructure {

    private val cache = new mutable.HashMap[Class[_], ClassObjectStructure]()

    def apply(objectClass: Class[_]): ClassObjectStructure = {
        cache.getOrElseUpdate(objectClass, new ClassObjectStructure(objectClass))
    }
}