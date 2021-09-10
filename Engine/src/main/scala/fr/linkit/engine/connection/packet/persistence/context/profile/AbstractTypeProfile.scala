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

import fr.linkit.api.connection.packet.persistence.context.TypeProfile
import fr.linkit.engine.local.utils.ScalaUtils

import java.lang.reflect.{Field, Modifier}

abstract class AbstractTypeProfile[T](override val typeClass: Class[_]) extends TypeProfile[T] {


}
