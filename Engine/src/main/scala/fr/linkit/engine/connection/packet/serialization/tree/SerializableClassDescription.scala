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

package fr.linkit.engine.connection.packet.serialization.tree

import fr.linkit.engine.local.utils.NumberSerializer

import java.lang.reflect.{Field, Modifier}

class SerializableClassDescription(val clazz: Class[_]) {

    val serializableFields: List[Field] = listSerializableFields(clazz)
    val signItemCount     : Int         = serializableFields.length - 1
    val classSignature    : Array[Byte] = NumberSerializer.serializeInt(clazz.getName.hashCode)

    private def listSerializableFields(cl: Class[_], subClassFields: Seq[Field] = List()): List[Field] = {
        if (cl == null)
            return List()

        def linkDescendant(field: Field): Boolean = {
            field.isAnnotationPresent(classOf[LinkDescendant]) &&
                    subClassFields.exists(subField => field.getType == subField.getType && field.getName == subField.getName)
        }

        val initial = cl.getDeclaredFields
                .filterNot(p => Modifier.isTransient(p.getModifiers) || Modifier.isStatic(p.getModifiers) || linkDescendant(p))
                .tapEach(_.setAccessible(true))
                .toList

        initial ++ listSerializableFields(cl.getSuperclass, initial) ++ cl.getInterfaces.flatMap(listSerializableFields(_, initial))
    }

    def foreachDeserializableFields(action: (Int, Field) => Unit): Unit = {
        var i = 0
        serializableFields.foreach(field => {
            action(i, field)

            var superClass = clazz.getSuperclass
            while (superClass != null) {
                val superField = superClass.getDeclaredFields.find(_.getName == field.getName)
                if (superField.exists(f => f.getType == field.getType && f.isAnnotationPresent(classOf[LinkDescendant])))
                    action(i, superField.get)
                superClass = superClass.getSuperclass
            }
            i += 1
        })
    }

    override def toString: String = s"SerializableClassDescription($clazz, $serializableFields)"

}
