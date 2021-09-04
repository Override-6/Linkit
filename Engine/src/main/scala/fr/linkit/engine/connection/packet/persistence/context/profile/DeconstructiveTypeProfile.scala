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

import fr.linkit.engine.connection.packet.persistence.context.profile.ConstructorTypeProfile.getConstructor

import java.lang.reflect.Constructor

class DeconstructiveTypeProfile[D <: Deconstructive](clazz: Class[_], constructor: Constructor[D]) extends ConstructorTypeProfile[D](clazz: Class[_], constructor, _.deconstruct()) {
    def this(clazz: Class[_]) {
        this(clazz, getConstructor[D](clazz))
    }
}