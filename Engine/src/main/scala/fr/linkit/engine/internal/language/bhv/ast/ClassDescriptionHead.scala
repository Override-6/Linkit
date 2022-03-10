package fr.linkit.engine.internal.language.bhv.ast

import fr.linkit.api.gnom.cache.sync.contract.RemoteObjectInfo

case class ClassDescriptionHead(static: Boolean, className: String,
                                referent: Option[InternalReference], remoteObjectInfo: Option[RemoteObjectInfo])

case class ClassDescription(head: ClassDescriptionHead,
                            foreachMethod: Option[MethodDescription],
                            foreachField: Option[FieldDescription],
                            methods: Seq[AttributedMethodDescription],
                            fields: Seq[AttributedFieldDescription])