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

package fr.linkit.engine.connection.packet.persistence.tree.nodes

import fr.linkit.api.connection.packet.persistence.tree._
import fr.linkit.engine.connection.packet.persistence.tree.DefaultSerialContext.ByteHelper
import fr.linkit.engine.connection.packet.persistence.tree._
import fr.linkit.engine.connection.packet.persistence.tree.nodes.ObjectNode.NullObjectFlag
import fr.linkit.engine.local.utils.UnWrapper.unwrap
import fr.linkit.engine.local.utils.NumberSerializer
import fr.linkit.engine.local.utils.NumberSerializer.{deserializeFlaggedNumber, serializeNumber}

import java.lang
import java.lang.reflect.{Array => RArray}

//FIXME does not handle primitive arrays
object ArrayNode extends NodeFactory[Array[_]] {

    val ArrayFlag: Byte = -54
    val EmptyFlag: Byte = -53

    override def canHandle(clazz: Class[_]): Boolean = {
        clazz.isArray
    }

    override def canHandle(bytes: ByteSeq): Boolean = {
        bytes.isClassDefined && bytes.sameFlagAt(5, ArrayFlag)
    }

    override def newNode(finder: NodeFinder, profile: ClassProfile[Array[_]]): SerialNode[Array[_]] = {
        new ArraySerialNode(profile, finder)
    }

    override def newNode(finder: NodeFinder, bytes: ByteSeq): DeserialNode[Array[_]] = {
        new ArrayDeserialNode(finder.getProfile[Array[_]], bytes, finder)
    }

    class ArraySerialNode(profile: ClassProfile[Array[_]], finder: NodeFinder) extends SerialNode[Array[_]] {

        override def serialize(array: Array[_], putTypeHint: Boolean): Array[Byte] = {
            //println(s"Serializing array ${array.mkString("Array(", ", ", ")")}")
            profile.applyAllSerialProcedures(array)

            val (compType, depth) = getAbsoluteCompType(array)
            val arrayTypeBytes    = NumberSerializer.serializeInt(compType.getName.hashCode)
            val head              = arrayTypeBytes :+ depth :+ ArrayFlag
            if (array.isEmpty) {
                return head ++ ArrayFlag /\ EmptyFlag
            }
            val lengths    = new Array[Int](array.length - 1) //Discard the last field, we already know its length by deducting it from previous lengths
            val byteArrays = new Array[Array[Byte]](array.length)

            var lastClass: Class[_]      = null
            var lastNode : SerialNode[_] = null

            var i = 0
            for (item <- array) {
                serializeItem(item)
                i += 1
            }

            def serializeItem(item: Any): Unit = {
                if (item == null) {
                    byteArrays(i) = Array(NullObjectFlag)
                    if (i != lengths.length)
                        lengths(i) = 1
                    return
                }
                val itemClass  = item.getClass
                val typeChange = itemClass != lastClass
                if (typeChange)
                    lastNode = finder.getSerialNodeForType(itemClass)
                //println(s"Serializing array item $item ($i)")
                val bytes = lastNode.serialize(cast(item), typeChange) //if type have changed, we need to specify the new type.
                //println(s"array item $item into bytes is now ${bytes.mkString("Array(", ", ", ")")}")

                byteArrays(i) = bytes
                if (i != lengths.length)
                    lengths(i) = bytes.length

                lastClass = itemClass
            }

            //println(s"lengths = ${lengths.mkString("Array(", ", ", ")")}")
            val sign       = lengths.flatMap(i => serializeNumber(i, true))
            //println(s"array sign = ${toPresentableString(sign)}")
            val signLength = serializeNumber(lengths.length, true)
            //println(s"arrayTypeCode = ${arrayTypeCode}")
            //println(s"array.getClass.getComponentType = ${array.getClass.getComponentType}")
            val result     = head ++ signLength ++ sign ++ byteArrays.flatten
            //println(s"result = ${toPresentableString(result)}")
            result
        }

        /**
         *
         * @param array the array to test
         * @return a tuple where the left index is the absolute component type of the array and the right index
         *         is the depth of the absolute component type in the array
         */
        private def getAbsoluteCompType(array: Array[_]): (Class[_], Byte) = {
            var i    : Byte     = Byte.MinValue
            var clazz: Class[_] = array.getClass
            while (clazz.isArray) {
                i = (i + 1).toByte
                clazz = clazz.componentType()
            }
            (clazz, i)
        }

        private def cast[T](any: Any): T = {
            any.asInstanceOf[T]
        }
    }

    class ArrayDeserialNode(profile: ClassProfile[Array[_]], bytes: ByteSeq, finder: NodeFinder) extends DeserialNode[Array[_]] {

        override def deserialize(): Array[_] = {
            val compType   = bytes.getClassOfSeq
            val arrayDepth = bytes(4) + Byte.MaxValue - 1
            if (bytes(7) == EmptyFlag)
                return buildArray(compType, arrayDepth, 0)

            //val classIdentifier = deserializeInt(bytes, 1)
            /*val arrayType = ClassMappings.getClass(classIdentifier)
            if (arrayType == null)
                throw new ClassNotMappedException(s"Unknown class identifier '$classIdentifier'")*/

            //starting from 6 because firsts bytes are the array type, depth and array flag.
            val (signItemCount, sizeByteCount: Byte) = deserializeFlaggedNumber[Int](bytes, 6)
            //println(s"signItemCount = ${signItemCount}")
            //println(s"sizeByteCount = ${sizeByteCount}")

            val sign   = LengthSign.from(signItemCount, bytes, bytes.length, sizeByteCount + 6)
            val result = buildArray(compType, arrayDepth, sign.childrenBytes.length)
            var i      = 0
            for (childBytes <- sign.childrenBytes) {
                //println(s"ITEM Deserial $i:")
                val node   = finder.getDeserialNodeFor(childBytes)
                //println(s"node = ${node}")
                val v: Any = node.deserialize()
                putInArray(result, i, v)
                //Try(//println(s"array item deserialize result = ${result(i)}"))
                i += 1
            }

            profile.applyAllDeserialProcedures(result)
            result
        }

        private def putInArray(array: Array[_], idx: Int, value: Any): Unit = {
            val v = array.getClass.componentType()
            v match {
                case Integer.TYPE      => RArray.setInt(array, idx, unwrap(value, _.intValue))
                case lang.Byte.TYPE    => RArray.setByte(array, idx, unwrap(value, _.byteValue))
                case lang.Short.TYPE   => RArray.setShort(array, idx, unwrap(value, _.shortValue))
                case lang.Long.TYPE    => RArray.setLong(array, idx, unwrap(value, _.longValue))
                case lang.Double.TYPE  => RArray.setDouble(array, idx, unwrap(value, _.doubleValue))
                case lang.Float.TYPE   => RArray.setFloat(array, idx, unwrap(value, _.floatValue))
                case lang.Boolean.TYPE => RArray.setBoolean(array, idx, unwrap(value, _.booleanValue))
                case Character.TYPE    => RArray.setChar(array, idx, unwrap(value, _.charValue))
                case _                               => RArray.set(array, idx, value)
            }
        }

        private def buildArray(compType: Class[_], arrayDepth: Int, arrayLength: Int): Array[_] = {
            var finalCompType = compType
            for (_ <- 0 to arrayDepth) {
                finalCompType = finalCompType.arrayType()
            }
            RArray.newInstance(finalCompType, arrayLength).asInstanceOf[Array[_]]
        }

    }

}
