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

package fr.linkit.engine.connection.cache.repo

import fr.linkit.api.connection.cache.repo.{Chip, IllegalPuppetException, PuppetDescription, PuppetException}
import fr.linkit.api.connection.packet.Packet
import fr.linkit.engine.connection.packet.fundamental.RefPacket
import fr.linkit.engine.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.connection.packet.traffic.channel.request.ResponseSubmitter
import fr.linkit.engine.local.utils.ScalaUtils

import java.lang.reflect.Modifier
import scala.util.control.NonFatal

case class ObjectChip[S] private(owner: String,
                                 description: PuppetDescription[S],
                                 puppet: S) extends Chip[S] {

    override def updateField(fieldID: Int, value: Any): Unit = {
        description.getFieldDesc(fieldID)
                .filterNot(_.isHidden)
                .foreach(desc => ScalaUtils.setValue(puppet, desc.field, value))
    }

    override def updateAllFields(obj: S): Unit = {
        description.listFields()
                .foreach(desc => if (!desc.isHidden) {
                    val field = desc.field
                    val value = field.get(obj)
                    ScalaUtils.setValue(puppet, field, value)
                })
    }

    override def callMethod(methodID: Int, params: Seq[Any]): Any = {
        val methodDesc = description.getMethodDesc(methodID)
        if (methodDesc.exists(_.isHidden)) {
            throw new PuppetException(s"Attempted to invoke ${methodDesc.fold("unknown")(_ => "hidden")} method '${
                methodDesc.map(_.method.getName).getOrElse(s"(unknown method id '$methodID')")
            }(${params.mkString(", ")}) in class ${methodDesc.get.clazz}'")
        }
        methodDesc.get
                .method
                .invoke(puppet, params: _*)
    }

    private[repo] def handleBundle(packet: Packet, submitter: ResponseSubmitter): Unit = {
        packet match {
            case ObjectPacket((methodId: Int, args: Array[Any])) =>
                var result: Any = null
                val castedArgs  = ScalaUtils.slowCopy[Serializable](args)
                try {
                    result = callMethod(methodId, castedArgs)
                } catch {
                    case NonFatal(e) =>
                        //FIXME instance loop for serializers.
                        throw e
                    //result = ThrowableWrapper(e)
                }
                submitter
                        .addPacket(RefPacket(result))
                        .submit()

            case ObjectPacket((fieldId: Int, value: Any)) =>
                val fieldDesc = description.getFieldDesc(fieldId)
                fieldDesc
                        .filterNot(_.isHidden)
                        .getOrElse(
                            throw new PuppetException(s"Attempted to set hidden field '${
                                fieldDesc.getOrElse(s"(unknown field id '$fieldId')")
                            }' to value '$value'")
                        )
                val field = fieldDesc.get.field
                ScalaUtils.setValue(puppet, field, value)
        }
    }

}

object ObjectChip {

    def apply[S](owner: String, description: PuppetDescription[S], puppet: S): ObjectChip[S] = {
        if (puppet == null)
            throw new NullPointerException("puppet is null !")
        val clazz = puppet.getClass

        if (Modifier.isFinal(clazz.getModifiers))
            throw new IllegalPuppetException("Puppet can't be final.")

        new ObjectChip[S](owner, description, puppet)
    }

}
