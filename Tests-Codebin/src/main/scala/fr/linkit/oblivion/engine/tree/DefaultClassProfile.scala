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

package fr.linkit.oblivion.engine.tree

import fr.linkit.api.connection.network.Network
import fr.linkit.oblivion.api.tree.ClassProfile
import fr.linkit.oblivion.api.tree.procedure.Procedure
import fr.linkit.engine.connection.packet.persistence.v3.ClassDescription

import scala.collection.mutable.ListBuffer

class DefaultClassProfile[T](clazz: Class[T], context: DefaultSerialContext) extends ClassProfile[T] {

    override val desc       = new ClassDescription(clazz)
    private  val procedures = ListBuffer.empty[Procedure[T]]

    override def getProcedures: Seq[Procedure[T]] = procedures.toSeq

    override def addProcedure(procedure: Procedure[T]): Unit = procedures += procedure

    override def removeProcedure(procedure: Procedure[T]): Unit = procedures -= procedure

    override def applyAllSerialProcedures(t: T): Unit = {
        applyAllProcedures(t, _.beforeSerial)
    }

    override def applyAllDeserialProcedures(t: T): Unit = {
        applyAllProcedures(t, _.afterDeserial)
    }

    private def applyAllProcedures(t: T, action: Procedure[_ >: T] => (T, Network) => Unit): Unit = {
        val network = context.getNetwork.orNull
        if (t == null || network == null)
            return
        procedures.foreach(action(_)(t, network))
        val superClass = clazz.getSuperclass
        if (superClass == null)
            return
        context.getClassProfile(superClass)
                .getProcedures
                .foreach(action(_)(t, network))

        applyUpperInterfaceProcedures(clazz)

        def applyUpperInterfaceProcedures(clazz: Class[_ >: T]): Unit = {
            clazz.getInterfaces
                    .map(_.asInstanceOf[Class[_ >: T]])
                    .foreach(interface => {
                        context.getClassProfile(interface)
                                .getProcedures
                                .foreach(action(_)(t, network))
                        applyUpperInterfaceProcedures(interface)
                    })
        }
    }

}
