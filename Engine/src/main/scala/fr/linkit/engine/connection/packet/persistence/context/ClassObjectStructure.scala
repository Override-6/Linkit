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

package fr.linkit.engine.connection.packet.persistence.context

import fr.linkit.api.connection.packet.persistence.obj.ObjectStructure
import fr.linkit.engine.local.utils.{ScalaUtils, UnWrapper}

import java.lang.reflect.{Field, Modifier}
import scala.collection.mutable

class ClassObjectStructure private(objectClass: Class[_]) extends ObjectStructure {

    val fields: Array[Field] = ScalaUtils.retrieveAllFields(objectClass).filterNot(f => Modifier.isTransient(f.getModifiers))
    private val fieldTypes = fields.map(_.getType)

    override def isAssignable(fields: Array[Class[_]]): Boolean = {
        val structureFields = fieldTypes
        if (fields.length != structureFields.length)
            return false
        var i = 0
        while (i < structureFields.length) {
            if (!structureFields(i).isAssignableFrom(fields(i)))
                return false
            i += 1
        }
        true
    }

    override def isAssignable(fieldsValues: Array[Any]): Boolean = {
        if (fieldsValues.length != fieldTypes.length)
            return false
        var i = 0
        while (i < fieldTypes.length) {
            val value = fieldsValues(i)
            if (value != null) {
                val clazz = UnWrapper.getPrimitiveClass(value) //would convert the value's class to it's primitive class if the value is a primitive wrapper
                if (!fieldTypes(i).isAssignableFrom(clazz))
                    return false
            }
            i += 1
        }
        true
    }

    override def equals(other: Any): Boolean = other match {
        case that: ClassObjectStructure => (that eq this) || isAssignable(that.fieldTypes)
        case _                          => false
    }
}

object ClassObjectStructure {

    private val cache = new mutable.HashMap[Class[_], ClassObjectStructure]()

    def apply(objectClass: Class[_]): ClassObjectStructure = {
        cache.getOrElseUpdate(objectClass, new ClassObjectStructure(objectClass))
    }
}
