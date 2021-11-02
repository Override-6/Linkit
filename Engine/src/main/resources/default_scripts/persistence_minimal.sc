import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.engine.gnom.network.{NetworkDataBundle, NetworkDataTrunk}
import fr.linkit.engine.gnom.packet.fundamental.EmptyPacket
import fr.linkit.engine.gnom.persistence.context.PersistenceConfigBuilder
import fr.linkit.engine.gnom.persistence.defaults.{IterableTypePersistence, MapTypePersistence}

import scala.collection.mutable

//Start Of Context
val builder: PersistenceConfigBuilder = null
val app    : ApplicationContext       = null
val traffic: PacketTraffic            = null

import builder._

//ENd Of Context

val connection = traffic.connection
putContextReference(1, EmptyPacket)
putContextReference(2, Nil)
putContextReference(3, None)
putContextReference(4, app)
putContextReference(5, traffic)
putContextReference(6, connection)
setTConverter[NetworkDataTrunk, NetworkDataBundle](_.toBundle)(NetworkDataTrunk.fromData)
addPersistence(new IterableTypePersistence)
addPersistence(new MapTypePersistence)
//setTConverter[File, String](_.getAbsolutePath)(new File(_))
//setTConverter[Date, Long](_.getTime)(new Date(_))
//setTConverter[Timestamp, Long](_.getTime)(new Timestamp(_))