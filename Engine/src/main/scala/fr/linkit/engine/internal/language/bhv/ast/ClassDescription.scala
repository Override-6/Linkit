package fr.linkit.engine.internal.language.bhv.ast

sealed trait DescriptionKind

case object RegularDescription extends DescriptionKind

case class MirroringDescription(stub: String) extends DescriptionKind

case class ChipDescription(stub: String) extends DescriptionKind

case object StaticsDescription extends DescriptionKind

case class ClassDescriptionHead(kinds: Seq[DescriptionKind], className: String)

case class ClassDescription(head: ClassDescriptionHead,
                            foreachMethod: Option[MethodDescription],
                            foreachField: Option[FieldDescription],
                            fields: Seq[AttributedFieldDescription],
                            methods: Seq[AttributedMethodDescription])