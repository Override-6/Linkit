package fr.linkit.api.gnom.cache.sync.contract.behavior

sealed trait EngineTag

case class IdentifierTag(identifier: String) extends EngineTag

case class ConstantTag private[behavior](name: String) extends EngineTag

