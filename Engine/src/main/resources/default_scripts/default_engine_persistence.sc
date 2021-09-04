import fr.linkit.engine.connection.packet.SimplePacketAttributes
import fr.linkit.engine.connection.packet.persistence.v3.persistor.{IterablePersistor, JavaCollectionPersistor, JavaMapPersistor, ScalaMapPersistor}

import java.io.File
import java.nio.file.Path
import java.util.Date

putMiniPersistor[File, String](_.getAbsolutePath)(new File(_))
putMiniPersistor[Path, String](_.toString)(Path.of(_))
putMiniPersistor[Date, Long](_.getTime)(new Date(_))

putProcedure[SimplePacketAttributes](c => println(s"attr of $c has been sent !"))(c => println(s"attr of $c has been received !"))

putPersistor(IterablePersistor)
putPersistor(ScalaMapPersistor)
putPersistor(JavaCollectionPersistor)
putPersistor(JavaMapPersistor)