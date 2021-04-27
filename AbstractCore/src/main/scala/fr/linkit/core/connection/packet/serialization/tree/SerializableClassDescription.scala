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

package fr.linkit.core.connection.packet.serialization.tree

import fr.linkit.core.local.utils.NumberSerializer

import java.lang.reflect.{Field, Modifier}

class SerializableClassDescription(val clazz: Class[_]) {

    val serializableFields: List[Field] = listSerializableFields(clazz)
    val signItemCount     : Int         = serializableFields.length - 1
    val classSignature    : Array[Byte] = NumberSerializer.serializeInt(clazz.getName.hashCode)

    private def listSerializableFields(cl: Class[_]): List[Field] = {
        if (cl == null)
            return List()

        val initial = cl.getDeclaredFields
                .filterNot(p => Modifier.isTransient(p.getModifiers) || Modifier.isStatic(p.getModifiers))
                .tapEach(_.setAccessible(true))
                .toList

        initial ++ listSerializableFields(cl.getSuperclass)
    }

}
