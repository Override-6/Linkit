package fr.linkit.engine.gnom.reference

import fr.linkit.api.application.packet.channel.request.RequestPacketChannel
import fr.linkit.api.gnom.reference.ReferencedObjectLocation
import fr.linkit.engine.application.packet.traffic.InternalPacketInjectableStore

private class ReferencedObjectsManager(channel: RequestPacketChannel, root: InternalPacketInjectableStore)
    extends NetworkObjectManager[ReferencedObject, ReferencedObjectLocation](channel) {
    override def isPresent(l: ReferencedObjectLocation): Boolean = {
        root.getPersistenceConfig(l.originChannelPath)
            .getReferenceStore
            .isPresent(l.refCode)
    }

    override def findLocation(objRef: ReferencedObject): Option[ReferencedObjectLocation] = {
        val channelPath = objRef.channelPath
        val obj         = objRef.obj
        val id          = root.getPersistenceConfig(channelPath)
            .getReferenceStore
            .findLocation(obj)
        if (id.isDefined) {
            Some(new SimpleReferencedObjectLocation(channelPath, id.get))
        } else None
    }

    override def findObject(location: ReferencedObjectLocation): Option[ReferencedObject] = {
        val obj = root.getPersistenceConfig(location.originChannelPath)
            .getReferenceStore
            .findObject(location: ReferencedObjectLocation)
        if (obj.isDefined) {
            Some(new ReferencedObject(obj.get, location.originChannelPath))
        } else None
    }
}