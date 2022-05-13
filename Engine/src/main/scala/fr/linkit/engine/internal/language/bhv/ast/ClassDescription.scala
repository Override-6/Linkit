package fr.linkit.engine.internal.language.bhv.ast

import fr.linkit.api.gnom.cache.sync.contract.SyncLevel

sealed trait DescriptionKind {
    val syncLevel: SyncLevel
}

case object SyncDescription extends DescriptionKind {
    override val syncLevel: SyncLevel = SyncLevel.Synchronized
}

case class MirroringDescription(stub: String) extends DescriptionKind {
    override val syncLevel: SyncLevel = SyncLevel.Mirroring
}

case class ChipDescription(stub: String) extends DescriptionKind {
    override val syncLevel: SyncLevel = SyncLevel.ChippedOnly
}

case object StaticsDescription extends DescriptionKind {
    override val syncLevel: SyncLevel = SyncLevel.Statics
}

case class ClassDescriptionHead(kinds: Seq[DescriptionKind], className: String)

case class ClassDescription(head: ClassDescriptionHead,
                            foreachMethod: Option[MethodDescription],
                            foreachField: Option[FieldDescription],
                            fields: Seq[AttributedFieldDescription],
                            methods: Seq[AttributedMethodDescription])