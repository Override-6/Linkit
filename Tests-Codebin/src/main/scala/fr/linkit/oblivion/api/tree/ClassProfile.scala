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

package fr.linkit.oblivion.api.tree
import fr.linkit.api.connection.packet.persistence.v3.SerializableClassDescription
import fr.linkit.oblivion.api.tree.procedure.Procedure

trait ClassProfile[T] {

    val desc: SerializableClassDescription

    //FIXME make this method protected
    def getProcedures: Seq[Procedure[T]]

    def removeProcedure(procedure: Procedure[T]): Unit

    def addProcedure(procedure: Procedure[T]): Unit

    def applyAllSerialProcedures(t: T): Unit

    def applyAllDeserialProcedures(t: T): Unit

}
