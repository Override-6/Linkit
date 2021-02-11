package fr.`override`.linkit.api.packet.serialization

import java.lang.reflect.{Field, Modifier}
import java.lang.{Boolean => JBoolean, Byte => JByte, Double => JDouble, Float => JFloat, Integer => JInt, Long => JLong, Short => JShort}

import fr.`override`.linkit.api.packet.Packet
import fr.`override`.linkit.api.packet.serialization.PacketSerializer._
import org.jetbrains.annotations.Nullable
import sun.misc.Unsafe

abstract class PacketSerializer {

    protected val signature: Array[Byte]

    def serialize(any: Any): Array[Byte] = {
        val t0 = System.currentTimeMillis()
        val bytes = signature ++ serializeObject(any)
        val t1 = System.currentTimeMillis()
        //println(s"Serialisation took ${t1 - t0}ms")
        bytes
    }

    def deserialize(bytes: Array[Byte]): Any = {
        if (!isSameSignature(bytes))
            throw new IllegalArgumentException("Those bytes does not come from this packet serializer !")

        val t0 = System.currentTimeMillis()
        val instance = deserializeObject(bytes.drop(signature.length)).asInstanceOf[Packet]
        val t1 = System.currentTimeMillis()
        //println(s"Deserialization took ${t1 - t0}ms")
        instance
    }

    def deserializeAll(bytes: Array[Byte]): Array[Any] = {
        deserializeArray(bytes.drop(signature.length))
    }

    def isSameSignature(bytes: Array[Byte]): Boolean = {
        bytes.startsWith(signature)
    }

    protected def serializeType(clazz: Class[_]): Array[Byte]

    /**
     * @return a tuple with the Class and his value length into the array
     * */
    protected def deserializeType(bytes: Array[Byte]): (Class[_], Int)


    private def serializeObject(any: Any): Array[Byte] = {
        if (any == null)
            return NullFlagArray
        if (!any.isInstanceOf[Serializable])
            throw new IllegalArgumentException("Attempted to serialize a non-Serializable object !")

        val clazz = any.getClass
        if (clazz.isArray)
            return serializeArray(any.asInstanceOf[Array[_]])

        val fields = listSerializableFields(clazz)
        val fieldsLength = fields.length
        val typeIdBytes = serializeType(clazz)

        if (fieldsLength == 0)
            return typeIdBytes ++ EmptyFlagArray

        var bytes = Array[Byte]()
        val lengths = new Array[Int](fieldsLength - 1) //Discard the last field, we already know his length by deducting it from previous lengths
        for (i <- 0 until fieldsLength) {
            val field = fields(i)
            val value = field.get(any)
            val valueBytes = serializeValue(field.getType, value)
            bytes ++= valueBytes
            if (i != fieldsLength - 1) //ensure we don't hit the last field
                lengths(i) = valueBytes.length
        }
        //println(s"lengths = ${lengths.mkString("Array(", ", ", ")")}")

        val lengthsBytes = lengths.flatMap(serializeInt)
        typeIdBytes ++ lengthsBytes ++ bytes
    }

    private def getAppropriateFlag(any: Array[_]): Byte = {
        if (any.forall(n => n.isInstanceOf[Int] || n.isInstanceOf[Byte] || n.isInstanceOf[Short]))
            return IntArrayFlag
        if (any.forall(n => n.isInstanceOf[Double] || n.isInstanceOf[Long] || n.isInstanceOf[Float]))
            return LongArrayFlag
        AnyArrayFlag
    }

    private def serializeArray(anyArray: Array[_]): Array[Byte] = {
        if (anyArray == null)
            return NullFlagArray

        val flag = getAppropriateFlag(anyArray)
        val content: Array[Byte] = flag match {
            case IntArrayFlag =>
                var buff = Array[Byte]()
                buff ++= anyArray.flatMap(n => serializeInt(n.asInstanceOf[Int]))
                buff
            case LongArrayFlag =>
                var buff = Array[Byte]()
                buff ++= anyArray.flatMap(n => serializeLong(n.asInstanceOf[Long]))
                buff
            case AnyArrayFlag =>
                val lengths = new Array[Int](anyArray.length)
                var buff = Array[Byte]()

                for (i <- anyArray.indices) {
                    val any = anyArray(i)
                    val bytes = serializeValue(null, any)
                    lengths(i) = bytes.length
                    buff ++= bytes
                }
                val numOfLength = serializeInt(lengths.length)
                numOfLength ++ lengths.flatMap(serializeInt) ++ buff: Array[Byte]
        }
        Array(flag) ++ content
    }

    private def deserializeArray(bytes: Array[Byte]): Array[Any] = {
        val flag = bytes(0)
        val content = bytes.drop(1)
        var buff = Array[Any]()
        flag match {
            case NullFlag =>
                return null
            case IntArrayFlag =>
                buff ++= content.grouped(4).map(bytes => deserializeInt(bytes, 0))
            case LongArrayFlag =>
                buff ++= content.grouped(8).map(bytes => deserializeLong(bytes, 0))
            case AnyArrayFlag => {
                val numOfLengths = deserializeInt(content, 0)
                val lengths = readLengths(content, 4, numOfLengths, content.length)

                var currentItIndex = (numOfLengths + 1) * 4 //the sign length
                for (itLength <- lengths) {
                    val itemBytes = content.slice(currentItIndex, currentItIndex + itLength)
                    buff = buff.appended(deserializeValue(null, itemBytes))

                    currentItIndex += itLength
                }
            }
        }
        buff
    }

    protected def serializeLong(value: Long): Array[Byte] = {
        Array[Byte](
            ((value >> 56) & 0xff).toByte,
            ((value >> 48) & 0xff).toByte,
            ((value >> 40) & 0xff).toByte,
            ((value >> 32) & 0xff).toByte,
            ((value >> 24) & 0xff).toByte,
            ((value >> 16) & 0xff).toByte,
            ((value >> 8) & 0xff).toByte,
            ((value >> 0) & 0xff).toByte
        )
    }

    protected def serializeInt(value: Int): Array[Byte] = {
        Array[Byte](
            ((value >> 24) & 0xff).toByte,
            ((value >> 16) & 0xff).toByte,
            ((value >> 8) & 0xff).toByte,
            ((value >> 0) & 0xff).toByte
        )
    }

    private def serializeValue(@Nullable extractedFieldType: Class[_], value: Any): Array[Byte] = {
        if (value == null) {
            return NullFlagArray
        }
        val clazz = value.getClass
        //println(s"Serializing value : $value of type $clazz")
        val serializedValue = if (clazz.isArray) {
            serializeArray(value.asInstanceOf[Array[_]])
        } else {
            extractedFieldType match {
                case JByte.TYPE => serializeInt(value.asInstanceOf[Byte])
                case JShort.TYPE => serializeInt(value.asInstanceOf[Short])
                case JInt.TYPE => serializeInt(value.asInstanceOf[Int])
                case JFloat.TYPE => serializeInt(JFloat.floatToRawIntBits(value.asInstanceOf[Float]))
                case JDouble.TYPE => serializeLong(JDouble.doubleToRawLongBits(value.asInstanceOf[Double]))
                case JLong.TYPE => serializeLong(value.asInstanceOf[Long])
                case JBoolean.TYPE => Array(if (value.asInstanceOf[Boolean]) 1.toByte else 0.toByte)
                case s: Class[String] if s == classOf[String] => value.asInstanceOf[String].getBytes
                case s: Class[JShort] if s == classOf[JShort] => serializeInt(value.asInstanceOf[Int])
                case b: Class[JByte] if b == classOf[JByte] => serializeInt(value.asInstanceOf[Int])
                case i: Class[JInt] if i == classOf[JInt] => serializeInt(value.asInstanceOf[Int])
                case i: Class[JLong] if i == classOf[JLong] => serializeLong(value.asInstanceOf[Byte])
                case f: Class[JFloat] if f == classOf[JFloat] => serializeInt(JFloat.floatToRawIntBits(value.asInstanceOf[Float]))
                case d: Class[JDouble] if d == classOf[JDouble] => serializeLong(JDouble.doubleToRawLongBits(value.asInstanceOf[Double]))
                case b: Class[JBoolean] if b == classOf[JBoolean] => Array(if (value.asInstanceOf[Boolean]) 1.toByte else 0.toByte)
                case e: Class[Enum[_]] if EnumClass.isAssignableFrom(e) => serializeType(clazz) ++ value.asInstanceOf[Enum[_]].name().getBytes

                case _ => value match {
                    case b: JByte => NumberFlagArray ++ serializeInt(b.toInt)
                    case s: JShort => NumberFlagArray ++ serializeInt(s.toInt)
                    case i: JInt => NumberFlagArray ++ serializeInt(i)
                    case f: JFloat => FloatFlagArray ++ serializeInt(JFloat.floatToRawIntBits(f))
                    case d: JDouble => DoubleFlagArray ++ serializeLong(JDouble.doubleToRawLongBits(d))
                    case l: JLong => FloatFlagArray ++ serializeLong(l)
                    case b: JBoolean => NumberFlagArray ++ Array[Byte](if (b) 1 else 0)

                    case s: String => StringFlagArray ++ s.getBytes
                    case e: Enum[_] =>
                        val clazz = e.getClass
                        EnumFlagArray ++ serializeType(clazz) ++ e.name().getBytes
                    case o => serializeObject(o)
                }
            }
        }
        //println(s"Serialized Value = ${new String(serializedValue)}")
        serializedValue
    }

    protected def deserializeInt(bytes: Array[Byte], index: Int): Int = {
        //println("Deserializing int in zone " + new String(bytes.slice(index, index + 4)))
        (0xff & bytes(index)) << 24 |
                ((0xff & bytes(index + 1)) << 16) |
                ((0xff & bytes(index + 2)) << 8) |
                ((0xff & bytes(index + 3)) << 0)
    }

    private def deserializeLong(bytes: Array[Byte], index: Int): Long = {
        (0xff & bytes(index)) << 52 |
                (0xff & bytes(index + 1)) << 48 |
                (0xff & bytes(index + 2)) << 40 |
                (0xff & bytes(index + 3)) << 32 |
                (0xff & bytes(index + 4)) << 24 |
                ((0xff & bytes(index + 5)) << 16) |
                ((0xff & bytes(index + 6)) << 8) |
                ((0xff & bytes(index + 7)) << 0)
    }

    private def readLengths(bytes: Array[Byte], from: Int, numOfLengths: Int, lastLengthReference: Int): Array[Int] = {
        val valuesLengths = new Array[Int](numOfLengths)
        for (i <- 0 until numOfLengths) yield {
            val length = if (i != numOfLengths - 1) {
                deserializeInt(bytes, from + (4 * i))
            } else {
                //If we hit the last field, we deduct his length by calculating
                //the object length - total referenced lengths sum
                lastLengthReference - valuesLengths.sum
            }
            valuesLengths(i) = length
        }
        valuesLengths
    }

    private def deserializeObject(bytes: Array[Byte]): Any = {
        //println(s"Deserializing object ${new String(bytes)}")

        if (bytes sameElements NullFlagArray)
            return null

        if (isArray(bytes))
            return deserializeArray(bytes)

        val (kindClass, kindClassLength) = deserializeType(bytes)
        val instance = TheUnsafe.allocateInstance(kindClass)

        if (bytes.length - kindClassLength == EmptyFlagArray.length && bytes.endsWith(EmptyFlagArray)) {
            //println("EMPTY INSTANCE !")
            return instance
        }

        val objectLength = bytes.length
        val fields = listSerializableFields(kindClass)
        val fieldsNumbers = fields.length

        //Reading Instance Sign...
        val valuesLengths = readLengths(bytes, kindClassLength, fieldsNumbers, objectLength)
        //println(s"valuesLengths = ${valuesLengths.mkString("Array(", ", ", ")")}")

        //Writing values to the empty instance
        var currentValIndex = kindClassLength + (valuesLengths.length - 1) * 4 //the sign length
        for (i <- 0 until fieldsNumbers) {
            val field = fields(i)
            val fieldType = field.getType
            val valueLength = valuesLengths(i)
            //println(s"field = ${field}")
            //println(s"currentValIndex = ${currentValIndex}")
            //println(s"valueLength = ${valueLength}")
            val value = deserializeValue(fieldType, bytes.slice(currentValIndex, currentValIndex + valueLength))
            //println(s"value = ${value}")
            setValue(instance, field, value)

            currentValIndex += valueLength
        }
        instance
    }

    private def setValue(instance: Any, field: Field, value: Any): Unit = {
        val fieldOffset = TheUnsafe.objectFieldOffset(field)

        def casted[A]: A = value.asInstanceOf[A]

        field.getType match {
            case JInt.TYPE => TheUnsafe.putInt(instance, fieldOffset, casted[Int])
            case JByte.TYPE => TheUnsafe.putByte(instance, fieldOffset, casted[Int].toByte)
            case JShort.TYPE => TheUnsafe.putShort(instance, fieldOffset, casted[Int].toShort)
            case JLong.TYPE => TheUnsafe.putLong(instance, fieldOffset, casted[Long])
            case JDouble.TYPE => TheUnsafe.putDouble(instance, fieldOffset, casted[Double])
            case JFloat.TYPE => TheUnsafe.putFloat(instance, fieldOffset, casted[Double].toFloat)
            case JBoolean.TYPE => TheUnsafe.putBoolean(instance, fieldOffset, casted)
            case _ => TheUnsafe.putObject(instance, fieldOffset, casted)
        }
    }

    private def deserializeEnum[T <: Enum[T]](bytes: Array[Byte]): T = {
        val (clazz, length) = deserializeType(bytes)
        val enumName = new String(bytes.drop(length))
        Enum.valueOf(clazz.asInstanceOf[Class[T]], enumName)
    }

    private def deserializeValue[T](@Nullable fieldType: Class[_], bytes: Array[Byte]): Any = {
        //println(s"Deserializing value of type $fieldType, bytes = ${new String(bytes)}")
        if (bytes sameElements NullFlagArray) {
            null
        } else if (fieldType != null && fieldType.isArray) {
            deserializeArray(bytes)
        } else if (fieldType != null && EnumClass.isAssignableFrom(fieldType)) {
            deserializeEnum(bytes)
        } else {
            fieldType match {
                case _@(JByte.TYPE | JShort.TYPE | JInt.TYPE) => deserializeInt(bytes, 0)
                case JLong.TYPE => deserializeLong(bytes, 0)
                case JFloat.TYPE => JFloat.intBitsToFloat(deserializeInt(bytes, 0))
                case JDouble.TYPE => JDouble.longBitsToDouble(deserializeLong(bytes, 0))
                case JBoolean.TYPE => bytes(0) == 1
                case s: Class[String] if s == classOf[String] => new String(bytes)
                case _ => bytes(0) match {
                    //Flag length + int length
                    case IntegerFlag => if (bytes.length == 5) deserializeInt(bytes, 1) else deserializeLong(bytes, 1)
                    case StringFlag => new String(bytes.drop(1))
                    case FloatFlag => JFloat.intBitsToFloat(deserializeInt(bytes, 1))
                    case DoubleFlag => JDouble.longBitsToDouble(deserializeLong(bytes, 1))
                    case EnumFlag => deserializeEnum(bytes.drop(1))
                    case _ => deserializeObject(bytes)
                }
            }
        }
    }

    private def listSerializableFields(clazz: Class[_]): Array[Field] = {
        clazz.getDeclaredFields
                .filterNot(p => Modifier.isTransient(p.getModifiers) || Modifier.isStatic(p.getModifiers))
                .tapEach(_.setAccessible(true))
                .toArray
    }
}

object PacketSerializer {
    private val TheUnsafe = findUnsafe()
    private val EnumClass = classOf[Enum[_]]

    private val IntArrayFlag: Byte = -2
    private val LongArrayFlag: Byte = -3
    private val AnyArrayFlag: Byte = -4

    private val IntegerFlag: Byte = -6
    private val StringFlag: Byte = -7
    private val NullFlag: Byte = -8
    private val EmptyFlag: Byte = -9
    private val FloatFlag: Byte = -10
    private val DoubleFlag: Byte = -11
    private val EnumFlag: Byte = -12

    private val NumberFlagArray: Array[Byte] = Array(IntegerFlag)
    private val StringFlagArray: Array[Byte] = Array(StringFlag)
    private val NullFlagArray: Array[Byte] = Array(NullFlag)
    private val EmptyFlagArray: Array[Byte] = Array(EmptyFlag)
    private val FloatFlagArray: Array[Byte] = Array(FloatFlag)
    private val DoubleFlagArray: Array[Byte] = Array(DoubleFlag)
    private val EnumFlagArray: Array[Byte] = Array(EnumFlag)

    private val ArrayFlags: Array[Array[Byte]] = Array(Array(IntArrayFlag), Array(LongArrayFlag), Array(AnyArrayFlag))

    private def isArray(bytes: Array[Byte]): Boolean = {
        ArrayFlags.exists(bytes.startsWith(_))
    }


    @throws[IllegalAccessException]
    private def findUnsafe(): Unsafe = {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        for (field <- unsafeClass.getDeclaredFields) {
            if (field.getType eq unsafeClass) {
                field.setAccessible(true)
                return field.get(null).asInstanceOf[Unsafe]
            }
        }
        throw new IllegalStateException("No instance of Unsafe found")
    }
}
