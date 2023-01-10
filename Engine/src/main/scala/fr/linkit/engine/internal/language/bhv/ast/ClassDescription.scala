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

package fr.linkit.engine.internal.language.bhv.ast

import fr.linkit.api.gnom.cache.sync.contract.level.{ConcreteSyncLevel, MirrorableSyncLevel, SyncLevel}

sealed trait DescriptionKind

case class LeveledDescription(levels: List[DescriptionLevel]) extends DescriptionKind

object RegularDescription extends DescriptionKind

object StaticsDescription extends DescriptionKind

sealed trait DescriptionLevel {
    val syncLevel: SyncLevel
}

case object SynchronizeLevel extends DescriptionLevel {
    override val syncLevel: SyncLevel = ConcreteSyncLevel.Synchronized
}

case object ChipLevel extends DescriptionLevel {
    override val syncLevel: SyncLevel = MirrorableSyncLevel.Chipped
}

case class MirroringLevel(stub: Option[String]) extends DescriptionLevel {
    override val syncLevel: SyncLevel = MirrorableSyncLevel.Mirror
}

case class ClassDescriptionHead(kind: DescriptionKind, classNames: List[String])

case class ClassDescription(head: ClassDescriptionHead,
                            foreachMethod: Option[MethodDescription],
                            foreachField: Option[FieldDescription],
                            fields: Seq[AttributedFieldDescription],
                            methods: Seq[AttributedMethodDescription])