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

package fr.linkit.engine.gnom.cache.sync.contract.behavior.member

import fr.linkit.api.gnom.cache.sync.contract.behavior.member.field.{FieldBehavior, FieldModifier}
import fr.linkit.api.gnom.cache.sync.contract.description.FieldDescription
import org.jetbrains.annotations.Nullable

case class SyncFieldBehavior[F](desc: FieldDescription,
                                override val isActivated: Boolean,
                                @Nullable override val modifier: FieldModifier[F]) extends FieldBehavior[F] {
    override def getName: String = desc.javaField.getName

}
