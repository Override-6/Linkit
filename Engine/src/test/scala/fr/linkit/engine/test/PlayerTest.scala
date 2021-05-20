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

package fr.linkit.engine.test

import fr.linkit.api.connection.cache.repo.annotations.MethodControl

import scala.annotation.meta.getter

case class PlayerTest(@MethodControl(localOnly = true)@getter id: Int,
                      @MethodControl(localOnly = true)@getter owner: String,
                      @MethodControl(localOnly = true)@getter var name: String,
                      @MethodControl(localOnly = true)@getter var x: Long,
                      @MethodControl(localOnly = true)@getter var y: Long) extends Serializable {

    def this(other: PlayerTest) = {
        this(other.id, other.owner, other.name, other.x, other.y)
    }

    def getName: String = name

    @MethodControl(localOnly = true)
    override def toString: String = s"PlayerTest($id, $owner, $name, $x, $y)"
}
