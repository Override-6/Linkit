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

import fr.linkit.api.connection.packet.persistence.v3.procedure.{FieldCompleter, MiniPersistor, Procedure}

import java.net.URL
import scala.reflect.ClassTag

trait PacketPersistenceContext {

    def putPersistor(persistence: ObjectPersistor[_], classes: Seq[HandledClass]): Unit

    def putPersistor(persistence: ObjectPersistor[_]): Unit = putPersistor(persistence, persistence.handledClasses)

    def putProcedure[A: ClassTag](procedure: Procedure[A]): Unit

    def putMiniPersistor[A: ClassTag](miniPersistor: MiniPersistor[A, _])

    def putFieldCompleter[A: ClassTag](completer: FieldCompleter[A, _])

    def getDescription[A](clazz: Class[_]): SerializableClassDescription[A]

    def executeConfigScript(url: URL): Unit

}
