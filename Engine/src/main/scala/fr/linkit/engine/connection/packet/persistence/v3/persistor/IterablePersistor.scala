package fr.linkit.engine.connection.packet.persistence.v3.persistor

import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserializationProgression
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.ObjectDeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.ObjectSerializerNode
import fr.linkit.api.connection.packet.persistence.v3._
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.SimpleObjectDeserializerNode
import fr.linkit.engine.connection.packet.persistence.v3.helper.ArrayPersistence
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.SimpleObjectSerializerNode
import fr.linkit.engine.local.utils.{JavaUtils, ScalaUtils}

import scala.collection.{IterableFactory, IterableOps, SeqFactory, mutable}

object IterablePersistor extends ObjectPersistor[IterableOnce[_]] {
    override val handledClasses: Seq[HandledClass] = Seq(HandledClass(classOf[IterableOnce[_]], true, Seq(SerialisationMethod.Serial, SerialisationMethod.Deserial)))

    type CC[A] <: IterableOps[A, Seq, Seq[A]]
    private val companions = mutable.HashMap.empty[Class[_], Option[IterableFactory[CC]]]

    override def willHandleClass(clazz: Class[_]): Boolean = {
        findFactoryCompanion(clazz).isDefined
    }

    override def getSerialNode(obj: IterableOnce[_], desc: SerializableClassDescription, context: PersistenceContext, progress: SerialisationProgression): ObjectSerializerNode = {
        val node = ArrayPersistence.serialize(obj.iterator.toArray, progress)
        SimpleObjectSerializerNode(out => {
            out.writeClass(obj.getClass)
            node.writeBytes(out)
        })
    }

    override def getDeserialNode(desc: SerializableClassDescription, context: PersistenceContext, progress: DeserializationProgression): ObjectDeserializerNode = {
        //TODO support sequences even if no factory is not found.
        val builder = findFactoryCompanion(desc.clazz)
            .getOrElse(throw new UnsupportedOperationException(s"factory not found for seq ${desc.clazz.getName}"))
            .newBuilder[AnyRef]
        val ref     = builder.result()
        SimpleObjectDeserializerNode(ref) { in =>
            val content = in.readArray[AnyRef]()
            builder.addAll(content)
            val result = builder.result()
            if (!JavaUtils.sameInstance(result, ref))
                ScalaUtils.pasteAllFields(ref, result)
            ref
        }
    }

    private def findFactoryCompanion(clazz: Class[_]): Option[IterableFactory[CC]] = {
        companions.getOrElseUpdate(clazz, findFactory(clazz))
    }

    private def findFactory(seqType: Class[_]): Option[IterableFactory[CC]] = {
        try {
            val companionClass = Class.forName(seqType.getName + "$")
            val companion      = companionClass.getField("MODULE$").get(null)
            companion match {
                case e: IterableFactory[CC] => Option(e)
                case _                 => None
            }
        } catch {
            case _: ClassNotFoundException => None
        }
    }
}
