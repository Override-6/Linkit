/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.api.connection.packet.persistence.v3

import fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserializationProgression
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.ObjectDeserializerNode
import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationProgression
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.ObjectSerializerNode

/**
 * An ObjectPersistor is a class that will serialize or deserialize an object of a specific type.
 * @tparam A The type of the object that this persistor will handle
 */
trait ObjectPersistor[A] {

    /**
     * A Sequences of handles classes that this persistor will handle.
     * @see [[HandledClass]]
     */
    val handledClasses: Seq[HandledClass]

    /**
     * Specify if the class can be handled by the class.
     * This method is useful if the persistor want to discard some
     * classes from the handledClasses list.
     * @param clazz the class to test
     * @return true if the class will be handled, false instead.
     */
    def willHandleClass(clazz: Class[_]): Boolean = true

    /**
     * Serializes an object of type [[A]] and convert it into an [[ObjectSerializerNode]] that will later convert the object into a byte sequence.
     * Note: Try not to create any node in the [[ObjectSerializerNode.writeBytes()]] method
     * Note: The implementation have to put the object class's int code at the header of this deserialized object's node. //TODO Explain this in ClassMappings
     * @param obj the object to deserialize
     * @param desc the object class description
     * @param context the context of the serializer that requested this serialization
     * @param progress the progress of the serialization
     * @return an [[ObjectSerializerNode]] that should be used later if the object was not already serialized.
     *
     * @see [[SerializableClassDescription]]
     * @see [[PacketPersistenceContext]]
     * @see [[SerialisationProgression]]
     * @see [[ObjectSerializerNode]]
     */
    def getSerialNode(obj: A, desc: SerializableClassDescription, context: PacketPersistenceContext, progress: SerialisationProgression): ObjectSerializerNode

    /**
     * Creates an [[ObjectDeserializerNode]] that will later deserialize
     * an object of type [[A]] from a [[fr.linkit.api.connection.packet.persistence.v3.deserialisation.DeserializationInputStream]].
     * Note: Try not to create any node into the [[ObjectDeserializerNode.deserialize()]] method
     * @param desc the object's description based on the class retrieved by this object's header int. //TODO Explain this in ClassMappings
     * @param context the context of the serializer that requested this serialization
     * @param progress the progress of the serialization
     * @return a node that should be used later if the object was not already deserialized.
     *
     * @see [[SerializableClassDescription]]
     * @see [[PacketPersistenceContext]]
     * @see [[SerialisationProgression]]
     * @see [[ObjectSerializerNode]]
     */
    def getDeserialNode(desc: SerializableClassDescription, context: PacketPersistenceContext, progress: DeserializationProgression): ObjectDeserializerNode

    /**
     * @return `true` if the [[sortedDeserializedObjects]] should be called.
     */
    def useSortedDeserializedObjects: Boolean = false

    /**
     * <p>
     * This method is used once a full deserialization has been completed. <br>
     * It is called only if [[useSortedDeserializedObjects]] = true. <br>
     * Note: The method is not called if this persistor have not been used. <br>
     * Note the method may be called two times per deserialization : Once for object pool, and once for packet body
     * </p>
     * @param objects The sorted objects in a top-to-bottom way. <br>
     *                <p>
     *                Example : Let's say that your persistor handles all objects of type
     *                N and Y. During deserialization, objects are created from bottom-to-top, this means that
     *                for an object like this :
     *                <pre>
     *                    class N {
     *                       val y: Y
     *                    }
     *                </pre>
     *                The deserialization will first deserialize y in order to complete the creation of N.<br>
     *                </p>
     *                So, if N and Y must be initialized somewhere in a top-to-bottom way,<br>
     *                there is no way for persistors to retrieve this order.<br>
     *                Thus, the "objects" parameter is an array of every deserialized objects from the last one to the firsts one.
     */
    def sortedDeserializedObjects(objects: Array[Any]): Unit = ()
}
