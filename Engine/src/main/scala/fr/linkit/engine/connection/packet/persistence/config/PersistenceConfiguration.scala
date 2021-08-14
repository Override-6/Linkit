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

package fr.linkit.engine.connection.packet.persistence.config

import fr.linkit.api.connection.packet.persistence.v3.{ObjectPersistor, PacketPersistenceContext}
import fr.linkit.api.connection.packet.persistence.v3.procedure.{MiniPersistor, Procedure}
import fr.linkit.engine.local.script.ScriptFile

import scala.reflect.ClassTag

abstract class PersistenceConfiguration(override protected val context: PacketPersistenceContext) extends PersistenceConfigurationMethods with ScriptFile {

    override protected def putMiniPersistor[A: ClassTag, B](serial: A => B)(deserial: B => A): Unit = {
        /*context.putMiniPersistor[A](new MiniPersistor[A, B] {
            override def serialize(a: A): B = serial(a)

            override def deserialize(b: B): A = deserial(b)
        })*/
    }

    override protected def putProcedure[A: ClassTag](serial: A => Unit)(deserial: A => Unit): Unit = {
        /*context.putProcedure[A](new Procedure[A] {
            override def onSerialized(obj: A): Unit = if (serial != null) serial(obj)

            override def onDeserialized(obj: A): Unit = if (deserial != null) deserial(obj)
        })*/
    }

    override protected def putPersistor(persistor: ObjectPersistor[_]): Unit = {
        //context.putPersistor(persistor)
    }

    /*protected def putFieldCompleter[A : ClassTag, B](complete: A => B): Unit = {
        context.putFieldCompleter[A](new FieldCompleter[A, B] {
            override def get(obj: A): Option[B] = Option(complete(obj))

            override def get(): B = ???
        })
    }*/

    @inline def configure(): Unit = {
        execute()
    }

}
