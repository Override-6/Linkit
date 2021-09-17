import fr.linkit.api.connection.packet.traffic.PacketTraffic
import fr.linkit.api.local.ApplicationContext
import fr.linkit.engine.connection.packet.fundamental.EmptyPacket
import fr.linkit.engine.connection.packet.persistence.context.PersistenceConfigBuilder

import java.io.File
import java.sql.Timestamp
import java.util.Date

//Start Of Context
val builder: PersistenceConfigBuilder = null
val app    : ApplicationContext       = null
val traffic: PacketTraffic            = null

import builder._

//ENd Of Context

val connection = traffic.connection
putContextReference(0, None)
putContextReference(1, EmptyPacket)
putContextReference(2, Nil)
putContextReference(3, app)
putContextReference(4, connection)
setTNewConverter[File, String](_.getAbsolutePath)(new File(_))
setTNewConverter[Date, Long](_.getTime)(new Date(_))
setTNewConverter[Timestamp, Long](_.getTime)(new Timestamp(_))