import fr.linkit.api.application.ApplicationContext
import fr.linkit.engine.gnom.network.{NetworkDataBundle, NetworkDataTrunk}
import fr.linkit.engine.gnom.packet.fundamental.EmptyPacket
import fr.linkit.engine.gnom.persistence.context.PersistenceConfigBuilder
import fr.linkit.engine.gnom.persistence.defaults._
import fr.linkit.engine.internal.language.bhv.interpreter.LangContractDescriptorData
import fr.linkit.engine.internal.language.bhv.{Contract, PropertyClass}
import fr.linkit.engine.internal.utils.Identity

//Start Of Context
val builder: PersistenceConfigBuilder = null

import builder._

//ENd Of Context

putContextReference(1, EmptyPacket)
putContextReference(2, Identity(Nil))
putContextReference(3, None)
setTConverter[NetworkDataTrunk, NetworkDataBundle](_.toBundle)(NetworkDataTrunk.fromData)
setTConverter[LangContractDescriptorData, (ApplicationContext, String, PropertyClass)](d => (d.app, d.source, d.propertyClass)){case (app, f, p) => Contract(f)(app, p)}
//putPersistence(new ScalaIterableTypePersistence)
//putPersistence(new ScalaMapTypePersistence)
putPersistence(new JavaArrayListTypePersistence)
putPersistence(new JavaHashMapTypePersistence)
putPersistence(new JavaHashSetTypePersistence)
//setTConverter[File, String](_.getAbsolutePath)(new File(_))
//setTConverter[Date, Long](_.getTime)(new Date(_))
//setTConverter[Timestamp, Long](_.getTime)(new Timestamp(_))