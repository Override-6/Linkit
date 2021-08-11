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

case class HandledClass(className: String, extendedClassEnabled: Boolean, methods: Seq[SerialisationMethod]) {

    override def hashCode(): Int = className.hashCode

}

object HandledClass {
    implicit def fromClassName(pair: (String, (Boolean, Seq[SerialisationMethod]))): HandledClass = new HandledClass(pair._1, pair._2._1, pair._2._2)

    implicit def fromClass(pair: (Class[_], (Boolean, Seq[SerialisationMethod]))): HandledClass = new HandledClass(pair._1.getName, pair._2._1, pair._2._2)

    def apply(clazz: Class[_], extendedClassEnabled: Boolean, methods: Seq[SerialisationMethod]): HandledClass = new HandledClass(clazz.getName, extendedClassEnabled, methods)
    def apply(clazz: Class[_], extendedClassEnabled: Boolean): HandledClass = new HandledClass(clazz.getName, extendedClassEnabled, Seq.from(SerialisationMethod.values()))
}
