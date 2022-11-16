import fr.linkit.api.gnom.network.tag.{NetworkFriendlyEngineTag, UniqueTag}
import fr.linkit.api.gnom.network.Network
import fr.linkit.engine.gnom.network.NetworkDataTrunk
import fr.linkit.engine.gnom.network.NetworkDataTrunk.NetworkDataBundle
import fr.linkit.engine.gnom.packet.fundamental.EmptyPacket
import fr.linkit.engine.gnom.persistence.config.PersistenceConfigBuilder
import fr.linkit.engine.gnom.persistence.defaults._
import fr.linkit.engine.internal.language.bhv.ContractProvider
import fr.linkit.engine.internal.language.bhv.interpreter.LangContractDescriptorData
import fr.linkit.engine.internal.util.Identity
import sun.security.provider.DSAKeyPairGenerator.Current

import java.nio.file.Path

//Start Of Context
val builder: PersistenceConfigBuilder = null
val network: Network

import builder._

//ENd Of Context

putContextReference(1, EmptyPacket)
putContextReference(2, Identity(Nil))
putContextReference(3, None)

setTConverter[Path, String](_.toString)(Path.of(_))
setTConverter[NetworkDataTrunk, NetworkDataBundle](_.toBundle)(NetworkDataTrunk.fromData)
setTConverter[LangContractDescriptorData, (String, String)](d => (d.fileName, d.propertiesName)) { case (name, propName) => ContractProvider(name, propName) }
//handle special case of `Current` magic tag
setTConverter[UniqueTag with NetworkFriendlyEngineTag, UniqueTag with NetworkFriendlyEngineTag]({
    case Current => network.currentEngine.identifiers.head //we change 'Current' tag to the identifier tag of the current engine when we serialize it
    case tag     => tag
})(tag => tag) //we perform no change for deserialization.

//putPersistence(new ScalaIterableTypePersistence)
//putPersistence(new ScalaMapTypePersistence)
putPersistence(new JavaArrayListTypePersistence)
putPersistence(new JavaHashMapTypePersistence)
putPersistence(new JavaHashSetTypePersistence)



