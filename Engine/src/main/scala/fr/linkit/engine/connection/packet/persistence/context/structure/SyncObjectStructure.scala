package fr.linkit.engine.connection.packet.persistence.context.structure

import fr.linkit.api.connection.cache.obj.description.SyncNodeInfo
import fr.linkit.api.connection.packet.persistence.obj.ObjectStructure

class SyncObjectStructure(objectStruct: ObjectStructure) extends ObjectStructure {
    override def isAssignable(args: Array[Class[_]], from: Int, to: Int): Boolean = {
        objectStruct.isAssignable(args, 0, args.length - 1) && args.last.isAssignableFrom(classOf[SyncNodeInfo])
    }

    override def isAssignable(args: Array[Any], from: Int, to: Int): Boolean = {
        objectStruct.isAssignable(args, 0, args.length - 1) && args.last.isInstanceOf[SyncNodeInfo]
    }
}
