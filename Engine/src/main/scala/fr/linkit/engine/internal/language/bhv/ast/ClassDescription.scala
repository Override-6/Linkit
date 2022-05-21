package fr.linkit.engine.internal.language.bhv.ast

import fr.linkit.api.gnom.cache.sync.contract.SyncLevel

sealed trait DescriptionKind

case class LeveledDescription(levels: Seq[DescriptionLevel]) extends DescriptionKind

object RegularDescription extends DescriptionKind

object StaticsDescription extends DescriptionKind

sealed trait DescriptionLevel {
    val syncLevel: SyncLevel
}

case object SynchronizeLevel extends DescriptionLevel {
    override val syncLevel: SyncLevel = SyncLevel.Synchronized
}

case object ChipLevel extends DescriptionLevel {
    override val syncLevel: SyncLevel = SyncLevel.ChippedOnly
}

case class MirroringLevel(stub: Option[String]) extends DescriptionLevel {
    override val syncLevel: SyncLevel = SyncLevel.Mirror
}

case class ClassDescriptionHead(kind: DescriptionKind, className: String)

case class ClassDescription(head: ClassDescriptionHead,
                            foreachMethod: Option[MethodDescription],
                            foreachField: Option[FieldDescription],
                            fields: Seq[AttributedFieldDescription],
                            methods: Seq[AttributedMethodDescription])