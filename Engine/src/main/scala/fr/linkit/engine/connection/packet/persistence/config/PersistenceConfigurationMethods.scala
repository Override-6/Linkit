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

import scala.reflect.ClassTag

trait PersistenceConfigurationMethods {
    protected def putMiniPersistor[A : ClassTag, B](serial: A => B)(deserial: B => A): Unit
    protected def putProcedure[A : ClassTag](serial: A => Unit)(deserial: A => Unit): Unit
}

object PersistenceConfigurationMethods extends PersistenceConfigurationMethods {

    override def putMiniPersistor[A: ClassTag, B](serial: A => B)(deserial: B => A): Unit = ???

    override def putProcedure[A: ClassTag](serial: A => Unit)(deserial: A => Unit): Unit = ???
}
