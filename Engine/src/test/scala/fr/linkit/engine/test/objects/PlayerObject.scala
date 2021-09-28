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

package fr.linkit.engine.test.objects

import fr.linkit.api.gnom.cache.sync.behavior.annotation.BasicInvocationRule.BROADCAST
import fr.linkit.api.gnom.cache.sync.behavior.annotation.MethodControl

import scala.annotation.meta._

case class PlayerObject(@(MethodControl @setter)(BROADCAST) id: Int,
                        @(MethodControl @setter)(BROADCAST) owner: String,
                        @(MethodControl @setter)(BROADCAST) var name: String,
                        @(MethodControl @setter)(BROADCAST) var x: Long,
                        @(MethodControl @setter)(BROADCAST) var y: Long) extends Serializable {

    def this(other: PlayerObject) = {
        this(other.id, other.owner, other.name, other.x, other.y)
    }

    override def toString: String = s"PlayerObject($id, $owner, $name, $x, $y)"
}
