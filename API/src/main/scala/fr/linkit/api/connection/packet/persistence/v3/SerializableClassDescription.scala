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

package fr.linkit.api.connection.packet.persistence.v3

import fr.linkit.api.connection.packet.persistence.v3.SerializableClassDescription.Fields
import fr.linkit.api.connection.packet.persistence.v3.procedure.{MiniPersistor, Procedure}

import java.lang.reflect.Field

trait SerializableClassDescription[A] {

    val clazz             : Class[A]
    val serializableFields: List[Fields]
    val signItemCount     : Int
    val classCode         : Array[Byte]

    def foreachDeserializableFields(deserialize: (Int, Field, Any => Unit) => Unit)(pasteOnField: (Field, Any) => Unit): Unit

    def serialPersistor: ObjectPersistor[A]

    def deserialPersistor: ObjectPersistor[A]

    def miniPersistor: Option[MiniPersistor[A, _]]

    def procedure: Option[Procedure[A]]

}

object SerializableClassDescription {

    case class Fields(first: Field, linked: Seq[Field])
}
