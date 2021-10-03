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

package fr.linkit.engine.gnom.persistence.context.structure

import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.engine.internal.utils.UnWrapper

abstract class ArrayObjectStructure() extends ObjectStructure {

    val types: Array[Class[_]]

    override def isAssignable(fields: Array[Class[_]], from: Int, to: Int): Boolean = {
        val structureFields = types
        if (fields.length != to)
            return false
        if (to > structureFields.length)
            throw new ArrayIndexOutOfBoundsException(s"to > array length. ($to > ${fields.length})")
        var i = from
        while (i < to) {
            if (!structureFields(i).isAssignableFrom(fields(i)))
                return false
            i += 1
        }
        true
    }

    override def isAssignable(fieldsValues: Array[Any], from: Int, to: Int): Boolean = {
        val len = fieldsValues.length
        var i = from
        if (to > len)
            throw new ArrayIndexOutOfBoundsException(s"to > array length. ($to > ${len})")
        if (to > types.length)
            return false
        while (i < to) {
            val value = fieldsValues(i)
            if (value != null) {
                val tpe = types(i)
                if (!(tpe.isAssignableFrom(value.getClass) || tpe.isAssignableFrom(UnWrapper.getPrimitiveClass(value))))
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

    def apply(typess: Array[Class[_]]): ArrayObjectStructure = {
        new ArrayObjectStructure {
            override val types: Array[Class[_]] = typess
        }
    }
}
