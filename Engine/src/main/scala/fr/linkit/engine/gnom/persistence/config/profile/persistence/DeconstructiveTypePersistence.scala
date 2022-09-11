/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.persistence.config.profile.persistence

import fr.linkit.api.gnom.persistence.context.Deconstructible
import fr.linkit.engine.gnom.persistence.config.profile.persistence.ConstructorTypePersistence.getConstructor

import java.lang.reflect.Constructor

class DeconstructiveTypePersistence[D <: AnyRef with Deconstructible](constructor: Constructor[D])
        extends ConstructorTypePersistence[D](constructor, (_: D).deconstruct()) {
    def this(clazz: Class[_]) {
        this(getConstructor[D](clazz))
    }
}