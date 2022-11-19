import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.network.tag.{Current, NameTag, NetworkFriendlyEngineTag, UniqueTag}
import fr.linkit.engine.gnom.network.NetworkDataTrunk
import fr.linkit.engine.gnom.network.NetworkDataTrunk.NetworkDataBundle
import fr.linkit.engine.gnom.packet.fundamental.EmptyPacket
import fr.linkit.engine.gnom.persistence.config.PersistenceConfigBuilder
import fr.linkit.engine.gnom.persistence.defaults._
import fr.linkit.engine.internal.language.bhv.ContractProvider
import fr.linkit.engine.internal.language.bhv.interpreter.LangContractDescriptorData
import fr.linkit.engine.internal.util.Identity

import java.nio.file.Path

//Start Of Context
val builder: PersistenceConfigBuilder = null
val connection: ConnectionContext

import builder._

//ENd Of Context

def network = connection.network

bindReference(1, EmptyPacket)
bindReference(2, Identity(Nil))
bindReference(3, None)

setTConverter[Path, String](_.toString)(Path.of(_))
setTConverter[NetworkDataTrunk, NetworkDataBundle](_.toBundle)(NetworkDataTrunk.fromData)
setTConverter[LangContractDescriptorData, (String, String)](d => (d.fileName, d.propertiesName)) { case (name, propName) => ContractProvider(name, propName) }

//handle special case of the `Current` magic tag
lazy val currentNameTag = NameTag(network.connection.currentName)
setReplacement[Current.type](_ => currentNameTag) //we change 'Current' tag to the identifier tag of the current engine when we serialize it

//putPersistence(new ScalaIterableTypePersistence)
//putPersistence(new ScalaMapTypePersistence)
putPersistence(new JavaArrayListTypePersistor)
putPersistence(new JavaHashMapTypePersistor)
putPersistence(new JavaHashSetTypePersistor)



