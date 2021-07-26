package fr.linkit.engine.connection.packet.persistence.v3.persistor

import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserializationProgression
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.DeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.SerializerNode
import fr.linkit.api.connection.packet.persistence.v3.{HandledClass, ObjectPersistor, PersistenceContext, SerializableClassDescription}
import fr.linkit.engine.connection.packet.persistence.v3.deserialisation.node.SimpleObjectDeserializerNode
import fr.linkit.engine.local.utils.{JavaUtils, ScalaUtils}

import scala.collection.{SeqFactory, SeqOps, mutable}

class SequencePersistor extends ObjectPersistor[collection.Seq[_]] {
    override val handledClasses: Seq[HandledClass] = Seq(HandledClass(classOf[Seq[_]], true), HandledClass(classOf[mutable.Seq[_]], true))

    type CC[A] <: SeqOps[A, Seq, Seq[A]]
    private val companions = mutable.HashMap.empty[Class[_], Option[SeqFactory[CC]]]

    override def willHandleClass(clazz: Class[_]): Boolean = {
        findSeqFactoryCompanion(clazz).isDefined
    }

    override def getSerialNode(obj: collection.Seq[_], desc: SerializableClassDescription, context: PersistenceContext, progress: SerialisationProgression): SerializerNode = {
        out => {
            out.writeClass(obj.getClass)
            out.writeArray(obj.toArray).writeBytes(out)
        }
    }

    override def getDeserialNode(desc: SerializableClassDescription, context: PersistenceContext, progress: DeserializationProgression): DeserializerNode = {
        //TODO support sequences even if no factory is not found.
        val builder = findSeqFactoryCompanion(desc.clazz)
            .getOrElse(throw new UnsupportedOperationException(s"factory not found for seq ${desc.clazz.getName}"))
            .newBuilder[AnyRef]
        val ref     = builder.result()
        SimpleObjectDeserializerNode(ref.asInstanceOf[AnyRef]) { in =>
            val content = in.readArray[AnyRef]()
            builder.addAll(content)
            val result = builder.result()
            if (!JavaUtils.sameInstance(result, ref))
                ScalaUtils.pasteAllFields(ref, result)
            ref
        }
    }

    private def findSeqFactoryCompanion(clazz: Class[_]): Option[SeqFactory[CC]] = {
        companions.getOrElseUpdate(clazz, findFactory(clazz))
    }

    private def findFactory[CC[A] <: SeqOps[A, Seq, Seq[A]]](seqType: Class[_]): Option[SeqFactory[CC]] = {
        try {
            val companionClass = Class.forName(seqType.getName + "$")
            val companion      = companionClass.getField("MODULE$").get(null)
            //println(s"companion = ${companion}")
            companion match {
                case e: SeqFactory[CC] => Option(e)
                case _                 => None
            }
        } catch {
            case _: ClassNotFoundException => None
        }
    }
}
