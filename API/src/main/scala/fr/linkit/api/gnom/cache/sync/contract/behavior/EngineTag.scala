package fr.linkit.api.gnom.cache.sync.contract.behavior

sealed trait EngineTag

case class IdentifierTag(identifier: String) extends EngineTag

case class ConstantTag private(name: String) extends EngineTag

object EngineTags {
    final val CurrentEngine    = ConstantTag("current")
    final val OwnerEngine      = ConstantTag("owner")
    final val CacheOwnerEngine = ConstantTag("cache_owner")
    final val RootOwnerEngine  = ConstantTag("root_owner")
}