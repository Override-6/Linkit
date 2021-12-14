package fr.linkit.engine.gnom.persistence.incident

import fr.linkit.api.gnom.network.statics.StaticAccess
import fr.linkit.api.gnom.persistence.incident.IncidentHandler
import fr.linkit.engine.gnom.persistence.serializor.ClassNotMappedException
import fr.linkit.engine.internal.mapping.ClassMappings.ClassMappings
import fr.linkit.engine.internal.mapping.{ClassMappings, MappedClassInfo}

class DefaultIncidentHandler(statics: StaticAccess) extends IncidentHandler {
    override def handleUnknownClass(code: Int): Class[_] = {
        ClassMappings.findUnknownClassInfo(code) match {
            case Some(unknownClass) => handleClass(unknownClass)
            case None               =>
                val unknownClass = (statics[ClassMappings].findKnownClassInfo(code): Option[MappedClassInfo])
                    .getOrElse(throw new ClassNotMappedException(s"No class is bound to code $code"))
                ClassMappings.putUnknownClass(unknownClass)
                handleClass(unknownClass)
        }
    }

    protected def handleClass(clazz: MappedClassInfo): Class[_] = {
        ???
    }
}
