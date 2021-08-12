import fr.linkit.engine.connection.packet.persistence.config.PersistenceConfigurationMethods._

import java.io.File
import java.nio.file.Path
import java.util.Date

putMiniPersistor[File, String](_.getAbsolutePath)(new File(_))
putMiniPersistor[Path, String](_.toString)(Path.of(_))
putMiniPersistor[Date, Long](_.getTime)(new Date(_))