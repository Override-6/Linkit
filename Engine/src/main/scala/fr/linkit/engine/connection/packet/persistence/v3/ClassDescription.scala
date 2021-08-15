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

package fr.linkit.engine.connection.packet.persistence.v3

import fr.linkit.api.connection.packet.persistence.v3.SerializableClassDescription
import fr.linkit.api.connection.packet.persistence.v3.SerializableClassDescription.Fields
import fr.linkit.engine.connection.packet.persistence.v3.ClassDescription.Synthetic
import fr.linkit.engine.local.utils.NumberSerializer

import java.lang.reflect.{Field, Modifier}
import scala.collection.mutable

class ClassDescription private(val clazz: Class[_]) extends SerializableClassDescription {

    //println(s"New class description created for $clazz")
    val serializableFields: List[Fields] = listSerializableFields(clazz)
    val signItemCount     : Int          = serializableFields.length
    val classCode         : Array[Byte]  = NumberSerializer.serializeInt(clazz.getName.hashCode)

    override def foreachDeserializableFields(deserialize: (Int, Field, Any => Unit) => Unit)(pasteOnField: (Field, Any) => Unit): Unit = {
        var i = 0
        serializableFields.foreach(fields => {
            deserialize(i, fields.first, value => {
                pasteOnField(fields.first, value)
                fields.linked.foreach(pasteOnField(_, value))
            })
            i += 1
        })
    }

    override def toString: String = s"SerializableClassDescription($clazz, $serializableFields)"

    private def listSerializableFields(cl: Class[_]): List[Fields] = {
        if (cl == null)
            return List()

        def listAllSerialFields(cl: Class[_]): Seq[Field] = {
            if (cl == null)
                return Seq.empty
            val fields = cl.getDeclaredFields
            fields
                    .filterNot(p => Modifier.isTransient(p.getModifiers) || Modifier.isStatic(p.getModifiers) || ((p.getModifiers & Synthetic) == Synthetic))
                    //.tapEach(field => println(s"Field ${field.getName}: ${field.getType}"))
                    .tapEach(_.setAccessible(true))
                    .toList ++ listAllSerialFields(cl.getSuperclass)
        }

        listAllSerialFields(cl)
                .groupBy(f => f.getName -> f.getType)
                .map(fields => Fields(fields._2.head, fields._2.drop(1)))
                .toList
                .sortBy(_.first.getName)
        //}
    }

}

object ClassDescription {

    private val cache = mutable.HashMap.empty[Class[_], ClassDescription]

    def apply(clazz: Class[_]): ClassDescription = cache.getOrElseUpdate(clazz, new ClassDescription(clazz))

    private val Synthetic = 0x00001000
}

