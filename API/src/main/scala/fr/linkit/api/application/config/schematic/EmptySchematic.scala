/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact overridelinkit@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.api.application.config.schematic

import fr.linkit.api.application.ApplicationContext

class EmptySchematic[A <: ApplicationContext] extends AppSchematic[A] {

    override def setup(a: A): Unit = ()

    override val name: String = "empty"
}

object EmptySchematic {

    def apply[A <: ApplicationContext]: EmptySchematic[A] = new EmptySchematic()
}
