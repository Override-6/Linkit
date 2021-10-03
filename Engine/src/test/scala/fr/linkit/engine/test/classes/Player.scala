/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.test.classes

import fr.linkit.api.gnom.cache.sync.behavior.annotation.BasicInvocationRule.BROADCAST
import fr.linkit.api.gnom.cache.sync.behavior.annotation.{MethodControl => MC}

import scala.annotation.meta.setter

case class Player(@(MC@setter)(BROADCAST) id: Int,
                  @(MC@setter)(BROADCAST) owner: String,
                  @(MC@setter)(BROADCAST) var name: String,
                  @(MC@setter)(BROADCAST) var x: Int,
                  @(MC@setter)(BROADCAST) var y: Int) extends Serializable {

    private val own = this
    private val test = "on m'appelle l'auvni"

    def this(other: Player) = {
        this(other.id, other.owner, other.name, other.x, other.y)
    }

}
