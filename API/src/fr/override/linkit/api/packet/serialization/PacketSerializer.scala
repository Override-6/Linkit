package fr.`override`.linkit.api.packet.serialization

import java.lang.reflect.{Field, Modifier}
import java.lang.{Boolean => JBoolean, Byte => JByte, Double => JDouble, Float => JFloat, Integer => JInt, Long => JLong, Short => JShort}

import fr.`override`.linkit.api.packet.Packet
import fr.`override`.linkit.api.packet.serialization.PacketSerializer._
import sun.misc.Unsafe

abstract class PacketSerializer {

    protected val signature: Array[Byte]

    def serialize(packet: Packet): Array[Byte] = {
        val t0 = System.currentTimeMillis()
        val bytes = signature ++ serializeObject(packet)
        val t1 = System.currentTimeMillis()
        println(s"Serialisation took ${t1 - t0}")
        bytes
    }

    def deserialize(bytes: Array[Byte]): Packet = {
        if (!isSameSignature(bytes))
            throw new IllegalArgumentException("Those bytes does not come from this packet serializer !")

        val t0 = System.currentTimeMillis()
        val instance = deserializeObject(bytes.drop(signature.length)).asInstanceOf[Packet]
        val t1 = System.currentTimeMillis()
        println(s"Deserialization took ${t1 - t0}")
        instance

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
        if (!any.isInstanceOf[Serializable])
            throw new IllegalArgumentException("Attempted to serialize a non-Serializable object !")
        val clazz = any.getClass
        val fields = listSerializableFields(clazz)
        val fieldsLength = fields.length

        var bytes = Array[Byte]()
        val lengths = new Array[Int](fieldsLength - 1) //Discard the last field, we already know his length by deducting it from previous lengths
        for (i <- 0 until fieldsLength) {
            val field = fields(i)
            val value = field.get(any)
            val valueBytes = serializeValue(value)
            bytes ++= valueBytes
            if (i != fieldsLength - 1) //ensure we don't hit the last field
                lengths(i) = valueBytes.length
        }
        val lengthsBytes = lengths.flatMap(serializeInt)
        val typeIdBytes = serializeType(clazz)
        typeIdBytes ++ lengthsBytes ++ bytes
    }

    private def getAppropriateFlag(any: Array[_]): Int = {
        if (any.forall(n => n.isInstanceOf[Int] || n.isInstanceOf[Byte] || n.isInstanceOf[Short]))
            return IntArrayFlag
        if (any.forall(n => n.isInstanceOf[Double] || n.isInstanceOf[Long] || n.isInstanceOf[Float]))
            return LongArrayFlag
        AnyArrayFlag
    }

    private def serializeArray(anyArray: Array[_]): Array[Byte] = {
        val flag = getAppropriateFlag(anyArray)
        val flagBytes = serializeInt(flag)
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
                val lengths = Array[Int](anyArray.length)
                var buff = Array[Byte]()

                for (i <- anyArray.indices) {
                    val any = anyArray(i)
                    val bytes = serializeObject(any)
                    lengths(i) = bytes.length
                    buff ++= bytes
                }
                val numOfLength = serializeInt(lengths.length)
                numOfLength ++ lengths.flatMap(serializeInt) ++ buff: Array[Byte]
        }
        flagBytes ++ content
    }

    private def deserializeArray(bytes: Array[Byte]): Array[Any] = {
        val flag = deserializeInt(bytes, 0)
        val content = bytes.drop(4)
        var buff = Array[Any]()
        flag match {
            case IntArrayFlag =>
                buff ++= content.grouped(4).map(bytes => deserializeInt(bytes, 0))
            case LongArrayFlag =>
                buff ++= content.grouped(8).map(bytes => deserializeLong(bytes, 0))
            case AnyArrayFlag => {
                val numOfLengths = deserializeInt(content, 4)
                val lengths = readLengths(content, 4, numOfLengths, bytes.length)
                var currentItIndex = 0
                for (itLength <- lengths) {
                    val itemBytes = content.slice(currentItIndex, currentItIndex + itLength)
                    buff = buff.appended(deserializeObject(itemBytes))

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

    private def serializeValue(value: Any): Array[Byte] = {
        if (value == null) {
            return Array[Byte](-1)
        }
        val clazz = value.getClass
        if (clazz.isArray) {
            serializeArray(value.asInstanceOf[Array[_]])
        } else {
            clazz match {
                case JByte.TYPE | JShort.TYPE | JInt.TYPE => serializeInt(value.asInstanceOf[Int])
                case JFloat.TYPE | JDouble.TYPE => serializeLong(JDouble.doubleToRawLongBits(value.asInstanceOf[Double]))
                case JLong.TYPE => serializeLong(value.asInstanceOf[Long])
                case JBoolean.TYPE => Array(if (value.asInstanceOf[Boolean]) 1.toByte else 0.toByte)
                case _ => value match {
                    case b: JByte => serializeInt(b.toInt)
                    case s: JShort => serializeInt(s.toInt)
                    case i: JInt => serializeInt(i)
                    case f: JFloat => serializeLong(JDouble.doubleToRawLongBits(f.toDouble))
                    case d: JDouble => serializeLong(JDouble.doubleToRawLongBits(d))
                    case l: JLong => serializeLong(l)
                    case b: JBoolean => Array[Byte](if (b) 1 else 0)

                    case s: String => s.getBytes
                    case e: Enum[_] =>
                        val clazz = e.getClass
                        serializeType(clazz) ++ e.name().getBytes
                    case o => serializeObject(o)
                }
            }
        }
    }

    protected def deserializeInt(bytes: Array[Byte], index: Int): Int = {
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
        val (kindClass, kindClassLength) = deserializeType(bytes)

        val objectLength = bytes.length
        val fields = listSerializableFields(kindClass)
        val fieldsNumbers = fields.length

        //Reading Instance Sign...
        val valuesLengths = readLengths(bytes, kindClassLength, fieldsNumbers, objectLength)

        //Writing values to the empty instance
        var currentValIndex = kindClassLength + (valuesLengths.length - 1) * 4 //the sign length
        val instance = TheUnsafe.allocateInstance(kindClass)
        for (i <- 0 until fieldsNumbers) {
            val field = fields(i)
            val fieldType = field.getType
            val valueLength = valuesLengths(i)
            val value = deserializeValue(fieldType, bytes.slice(currentValIndex, currentValIndex + valueLength))
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
        Enum.valueOf(clazz.asInstanceOf[Class[T]], new String(bytes.drop(length)))
    }

    private def deserializeValue[T](fieldType: Class[_], bytes: Array[Byte]): Any = {
        if (bytes.length == 1 && bytes(0) == -1) { //This is a null signature !
            null
        } else if (fieldType.isArray) {
            deserializeArray(bytes)
        } else if (EnumClass.isAssignableFrom(fieldType)) {
            deserializeEnum(bytes)
        } else if (fieldType == classOf[String]) {
            new String(bytes)
        } else {
            fieldType match {
                case _@(JByte.TYPE | JShort.TYPE | JInt.TYPE) => deserializeInt(bytes, 0)
                case _@(JFloat.TYPE | JDouble.TYPE | JLong.TYPE) => deserializeLong(bytes, 0) //FIXME would not work as expected for Float and Double
                case JBoolean.TYPE => bytes(0) == 1
                case _ => deserializeObject(bytes)
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

    private val IntArrayFlag = 0x12f
    private val LongArrayFlag = 0x13d
    private val AnyArrayFlag = 0x15b

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
