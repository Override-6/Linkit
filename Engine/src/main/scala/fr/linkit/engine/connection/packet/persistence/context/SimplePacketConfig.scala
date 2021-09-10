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

package fr.linkit.engine.connection.packet.persistence.context

import fr.linkit.api.connection.packet.persistence.context.{PacketConfig, PersistenceContext, TypeProfile}

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

abstract class SimplePacketConfig extends PacketConfig {

    override def getReferenced(reference: Int): Option[AnyRef] = {
        references.codeToRef.get(reference)
    }

    override def getReferencedCode(reference: AnyRef): Option[Int] = {
        references.refToCode.get(reference)
    }

    override def getProfile[T](clazz: Class[_], context: PersistenceContext): TypeProfile[T] = {
        profiles.customProfiles.getOrElse(clazz, context.getDefaultProfile[T](clazz))
                .asInstanceOf[TypeProfile[T]]
    }

    object profiles {

        private[SimplePacketConfig] val customProfiles = mutable.HashMap.empty[Class[_], TypeProfile[_]]

        def +=[T: ClassTag](profile: TypeProfile[T]): this.type = {
            val clazz = classTag[T].runtimeClass
            customProfiles.put(clazz, profile)
            this
        }
    }

    object references {

        private[SimplePacketConfig] val codeToRef = mutable.HashMap.empty[Int, AnyRef]
        private[SimplePacketConfig] val refToCode = mutable.HashMap.empty[AnyRef, Int]

        def ++=(refs: AnyRef*): this.type = {
            refs.foreach(+=)
            this
        }

        def +=(anyRef: AnyRef): this.type = {
            +=(anyRef.hashCode(), anyRef)
        }

        def +=(code: Int, anyRef: AnyRef): this.type = {
            codeToRef.put(code, anyRef)
            refToCode.put(anyRef, code)
            this
        }
    }

    protected var unsafeUse     = true
    protected var withSignature = true
    protected var wide          = false

    override def useUnsafe: Boolean = unsafeUse

    override def putSignature: Boolean = withSignature

    override def widePacket: Boolean = wide
}
