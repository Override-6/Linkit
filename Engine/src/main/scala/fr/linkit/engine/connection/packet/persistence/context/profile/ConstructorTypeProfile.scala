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

package fr.linkit.engine.connection.packet.persistence.context.profile

import java.lang.reflect.Constructor
import fr.linkit.engine.connection.packet.persistence.context.profile
import fr.linkit.engine.connection.packet.persistence.context.profile.ConstructorTypeProfile.getConstructor

class ConstructorTypeProfile[T](clazz: Class[_], constructor: Constructor[T], deconstructor: T => Array[Any]) extends AbstractTypeProfile[T](clazz) {

    def this(clazz: Class[_], deconstructor: T => Array[Any]) {
        this(clazz, getConstructor[T](clazz), deconstructor)
    }

    override def newInstance(args: Array[Any]): T = {
        constructor.newInstance(args: _*)
    }

    override def toArray(t: T): Array[Any] = {
        deconstructor(t)
    }


}

object ConstructorTypeProfile {

    def getConstructor[T](clazz: Class[_]): Constructor[T] = {
        clazz.getConstructors
                .find(_.isAnnotationPresent(classOf[profile.Constructor]))
                .getOrElse {
                    throw new NoSuchElementException(s"No Constructor is annotated for $clazz, please specify a constructor or annotate one using @Constructor")
                }.asInstanceOf[Constructor[T]]
    }
}
