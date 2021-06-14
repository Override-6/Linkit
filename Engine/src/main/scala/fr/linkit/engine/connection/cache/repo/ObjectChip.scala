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

import fr.linkit.api.connection.cache.repo._
import fr.linkit.api.connection.cache.repo.description.PuppetDescription
import fr.linkit.api.connection.packet.Packet
import fr.linkit.engine.connection.packet.fundamental.RefPacket
import fr.linkit.engine.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.connection.packet.traffic.channel.request.ResponseSubmitter
import fr.linkit.engine.local.utils.ScalaUtils
import java.lang.reflect.Modifier
import java.util.NoSuchElementException

import scala.reflect.ClassTag
import scala.util.control.NonFatal
import scala.reflect.runtime.universe._

case class ObjectChip[S] private(owner: String,
                                 description: PuppetDescription[S],
                                 wrapper: S with PuppetWrapper[S]) extends Chip[S] {

    private val mirror = runtimeMirror(wrapper.getClass.getClassLoader).reflect(wrapper)

    override def updateField(fieldID: Int, value: Any): Unit = {
        description.getFieldDesc(fieldID)
            .filterNot(_.isHidden)
            .fold {
                throw new PuppetException(s"Attempted to set hidden field '${
                    description
                        .getFieldDesc(fieldID)
                        .getOrElse(s"(unknown field id '$fieldID')")
                }' to value '$value'")
            } { desc => mirror.reflectMethod(desc.fieldSetter)(value) }
    }

    override def updateAllFields(obj: S): Unit = {
        val objMirror = runtimeMirror(obj.getClass.getClassLoader)
            .reflect(obj)(ClassTag(obj.getClass))
        description.listFields()
            .foreach(desc => if (!desc.isHidden) {
                val getter = desc.fieldGetter
                val value  = objMirror.reflectMethod(getter)()
                mirror.reflectMethod(getter)(value)
            })
    }

    override def callMethod(methodID: Int, params: Seq[Any]): Any = {
        val methodDesc = description.getMethodDesc(methodID)
        if (methodDesc.forall(_.isHidden)) {
            throw new PuppetException(s"Attempted to invoke ${methodDesc.fold("unknown")(_ => "hidden")} method '${
                methodDesc.map(_.method.name.toString).getOrElse(s"(unknown method id '$methodID')")
            }(${params.mkString(", ")}) in class ${methodDesc.get.method}'")
        }
        wrapper.getChoreographer.forceLocalInvocation {
            mirror.reflectMethod(methodDesc.get.method)
                .apply(params)
        }
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

                updateField(fieldId, value)
        }
    }

}

object ObjectChip {

    def apply[S](owner: String, description: PuppetDescription[S], puppet: S with PuppetWrapper[S]): ObjectChip[S] = {
        if (puppet == null)
            throw new NullPointerException("puppet is null !")
        val clazz = puppet.getClass

        if (Modifier.isFinal(clazz.getModifiers))
            throw new IllegalPuppetException("Puppet can't be final.")

        new ObjectChip[S](owner, description, puppet)
    }

}
