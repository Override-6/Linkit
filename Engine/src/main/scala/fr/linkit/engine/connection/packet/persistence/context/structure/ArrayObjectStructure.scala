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

package fr.linkit.engine.connection.packet.persistence.context.structure

import fr.linkit.api.connection.packet.persistence.obj.ObjectStructure
import fr.linkit.engine.local.utils.UnWrapper

abstract class ArrayObjectStructure() extends ObjectStructure {

    val types: Array[Class[_]]

    override def isAssignable(fields: Array[Class[_]]): Boolean = {
        val structureFields = types
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
        if (fieldsValues.length != types.length)
            return false
        var i = 0
        while (i < types.length) {
            val value = fieldsValues(i)
            if (value != null) {
                val clazz = UnWrapper.getPrimitiveClass(value) //would convert the value's class to it's primitive class if the value is a primitive wrapper
                if (!types(i).isAssignableFrom(clazz))
                    return false
            }
            i += 1
        }
        true
    }

    override def equals(other: Any): Boolean = other match {
        case that: ClassObjectStructure => (that eq this) || isAssignable(that.types)
        case _                          => false
    }
    
}

object ArrayObjectStructure {

    def apply: ObjectStructureBuilder = new ObjectStructureBuilder()
}
