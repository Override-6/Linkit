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

import fr.linkit.api.connection.packet.persistence.v3.SerializableClassDescription.Fields
import fr.linkit.api.connection.packet.persistence.v3.procedure.{MiniPersistor, Procedure}
import fr.linkit.api.connection.packet.persistence.v3.{ObjectPersistor, SerializableClassDescription}
import fr.linkit.engine.connection.packet.persistence.v3.PersistenceClassDescription.Synthetic
import fr.linkit.engine.local.utils.NumberSerializer

import java.lang.reflect.{Field, Modifier}

class PersistenceClassDescription[A] private[v3](val clazz: Class[A], context: DefaultPacketPersistenceContext) extends SerializableClassDescription[A] {

    println(s"New class description created for $clazz.")
    val serializableFields: List[Fields] = listSerializableFields(clazz)
    val signItemCount     : Int          = serializableFields.length
    val classCode         : Array[Byte]  = NumberSerializer.serializeInt(clazz.getName.hashCode)

    private var procedure0    : Option[Procedure[A]]   = None
    private var miniPersistor0: Option[MiniPersistor[A, _]] = None

    override def procedure: Option[Procedure[A]] = procedure0

    override def miniPersistor: Option[MiniPersistor[A, _]] = miniPersistor0

    override def foreachDeserializableFields(deserialize: (Int, Field, Any => Unit) => Unit)(pasteOnField: (Field, Any) => Unit): Unit = {
        var i = 0
        serializableFields.foreach(fields => {
            deserialize(i, fields.first, value => {
                val mappedValue = miniPersistor0.map(_.deserialize(cast(value))).getOrElse(value)
                pasteOnField(fields.first, mappedValue)
                fields.linked.foreach(pasteOnField(_, mappedValue))
            })
            i += 1
        })
    }

    override def toString: String = s"SerializableClassDescription($clazz, $serializableFields)"

    override lazy val serialPersistor  : ObjectPersistor[A] = context.getPersistenceForSerialisation(clazz)
    override lazy val deserialPersistor: ObjectPersistor[A] = context.getPersistenceForDeserialisation(clazz)

    def setProcedure(procedure: Procedure[A]): Unit = this.procedure0 = Option(procedure)

    def setMiniPersistor(persistor: MiniPersistor[A, _]): Unit = miniPersistor0 = Option(persistor)

    private def cast[B](obj: Any): B = obj.asInstanceOf[B]

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

object PersistenceClassDescription {

    private val Synthetic = 0x00001000
}

