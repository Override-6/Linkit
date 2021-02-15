package fr.`override`.linkit.api.packet.serialization

import fr.`override`.linkit.api.packet.Packet
import fr.`override`.linkit.api.packet.serialization.PacketSerializer._
import org.jetbrains.annotations.Nullable
import sun.misc.Unsafe

import java.lang.reflect.{Field, Modifier}
import java.lang.{Boolean => JBoolean, Byte => JByte, Double => JDouble, Float => JFloat, Integer => JInt, Long => JLong, Short => JShort}

//TODO use smart switch between bytes, shorts and ints serial/deserialization in order to save like 2 times more space.
abstract class PacketSerializer {

    protected val signature: Array[Byte]
    //private var totalSerialTime: Long = 0
    //private var totalSerials: Float = 0F
    //private var totalBytesCreated: Float = 0F

    def serialize(any: Any): Array[Byte] = {
        //val t0 = System.currentTimeMillis()
        val bytes = signature ++ serializeObject(any)
        /*
        val t1 = System.currentTimeMillis()
        //println(s"Serialisation took ${t1 - t0}ms")
        totalSerialTime += t1 - t0
        //println(s"totalSerialTime = ${totalSerialTime}")
        totalSerials += 1
        totalBytesCreated += bytes.length
        //println(s"${getClass.getSimpleName} have a serial length Average of ${totalBytesCreated / totalSerials} bytes)
        */
        bytes
    }

    def deserialize(bytes: Array[Byte]): Any = {
        if (!isSameSignature(bytes))
            throw new IllegalArgumentException("Those bytes does not come from this packet serializer !")

        //val t0 = System.currentTimeMillis()
        val instance = deserializeObject(bytes.drop(signature.length)).asInstanceOf[Packet]
        /*val t1 = System.currentTimeMillis()
        totalSerialTime += t1 - t0*/
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
        println(s"Serializing object '$any'")
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
            return typeIdBytes ++ NoFieldFlagArray

        var bytes = Array[Byte]()
        //Discard the last field, we already know his length by deducting it from previous lengths
        val lengths = new Array[Int](fieldsLength - 1)
        //println(s"fieldsLength = ${fieldsLength}")

        for (i <- 0 until fieldsLength) {
            val field = fields(i)
            //println(s"field = ${field}")
            val value = field.get(any)
            println(s"Serializing field value '$value' of field $field")
            val valueBytes = serializeValue(field.getType, value)
            println(s"value = ${new String(valueBytes)}")
            println("Deserialized value: " + deserializeValue(field.getType, valueBytes))
            bytes ++= valueBytes
            println(s"i = ${i}")
            if (i != fieldsLength - 1) //ensure we don't hit the last field
                lengths(i) = valueBytes.length
            //println(s"lengths = ${lengths.mkString("Array(", ", ", ")")}")
        }
        //println(s"lengths = ${lengths.mkString("Array(", ", ", ")")}")

        //println(s"lengths = ${lengths.mkString("Array(", ", ", ")")}")
        val signBytes = if (fieldsLength == 1) OneFieldFlagArray else lengths.flatMap(l => serializeNumber(l, true))
        //println(s"signBytes = ${new String(signBytes).replace('\r', ' ')}")
        typeIdBytes ++ signBytes ++ bytes
    }

    private def getAppropriateFlag(any: Array[_]): Byte = {
        def casted[T](x: Any): T = x.asInstanceOf[T]

        def isNumber(x: Any): Boolean = x.isInstanceOf[Byte] || x.isInstanceOf[Short] || x.isInstanceOf[Int] || x.isInstanceOf[Long]

        if (any.forall(isNumber) && any.forall(a => Byte.MinValue <= casted(a) && Byte.MaxValue >= casted[Byte](a)))
            return ByteArrayFlag
        if (any.forall(isNumber) && any.forall(a => Short.MinValue <= casted(a) && Short.MaxValue >= casted[Short](a)))
            return ShortArrayFlag
        if (any.forall(isNumber) && any.forall(a => Int.MinValue <= casted(a) && Int.MaxValue >= casted[Long](a)))
            return IntArrayFlag
        if (any.forall(n => n.isInstanceOf[Double] || n.isInstanceOf[Long] || n.isInstanceOf[Float]))
            return LongArrayFlag
        AnyArrayFlag
    }

    private def serializeArray(anyArray: Array[_]): Array[Byte] = {
        if (anyArray == null)
            return NullFlagArray

        val flag = getAppropriateFlag(anyArray)
        //println(s"flag = ${flag}")
        val content: Array[Byte] = flag match {
            case ByteArrayFlag =>
                //println("THIS IS A BYTE ARRAY")
                anyArray.map(_.asInstanceOf[Byte])
            case ShortArrayFlag =>
                anyArray.map(null)
            case IntArrayFlag =>
                anyArray.flatMap(n => serializeInt(n.asInstanceOf[Int]))
            case LongArrayFlag =>
                anyArray.flatMap(n => serializeLong(n.asInstanceOf[Long]))
            case AnyArrayFlag =>
                val lengths = new Array[Int](anyArray.length - 1) //discard the last element length, because it can be deducted from the total length
                var buff = Array[Byte]()

                for (i <- anyArray.indices) {
                    val any = anyArray(i)
                    //println(s"serializing = ${any}")
                    val bytes = serializeValue(null, any)
                    //println(s"serialized $any as ${new String(bytes).replace('\r', ' ')}")
                    if (i < lengths.length) lengths(i) = bytes.length //Add the length only if it is not the last item
                    buff ++= bytes
                }
                val numOfLength = serializeNumber(anyArray.length, true)
                val signBytes = lengths.flatMap(l => serializeNumber(l, true))
                //println(s"For array : ${anyArray.mkString("Array(", ", ", ")")}")
                //println(s"signBytes = ${new String(signBytes)}")
                //println("buff = " + new String(buff).replace('\r', ' '))
                //println(s"lengths = ${lengths.mkString("Array(", ", ", ")")}")
                numOfLength ++ signBytes ++ buff
        }
        //println(s"content :${new String(content)}")
        Array(flag) ++ content
    }


    private def deserializeArray(bytes: Array[Byte]): Array[Any] = {
        //println(s"Deserializing ${new String(bytes).replace('\r', ' ')}")
        val flag = bytes(0)
        //println(s"flag = ${flag}")
        val content = bytes.drop(1)
        var buff = Array[Any]()
        flag match {
            case NullFlag =>
                return null
            case ByteArrayFlag =>
                buff ++= content
            case ShortArrayFlag =>
                buff ++= content.grouped(2).map(deserializeNumber(_, 0, 2).toShort)
            case IntArrayFlag =>
                buff ++= content.grouped(4).map(deserializeNumber(_, 0, 4).toInt)
            case LongArrayFlag =>
                buff ++= content.grouped(8).map(deserializeNumber(_, 0, 8))
            case AnyArrayFlag => {
                val (numOfLengths, numBytesLength) = deserializeFlaggedNumber(content, 0)
                //println(s"numOfLengths = ${numOfLengths}")
                //println(s"numBytesLength = ${numBytesLength}")
                //println(s"content = ${new String(content).replace('\r', ' ')}")
                val (lengths, signBytesLength) = readSign(content, numBytesLength, numOfLengths.toInt, content.length)
                //println(s"signBytesLength = ${signBytesLength}")
                var currentItIndex = signBytesLength + numBytesLength //the total sign length
                //println(s"lengths = ${lengths.mkString("Array(", ", ", ")")}")
                //println(s"signBytesLength = ${signBytesLength}")
                for (itLength <- lengths) {
                    //println(s"currentItIndex = ${currentItIndex}")
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

    protected def serializeShort(value: Short): Array[Byte] = {
        Array[Byte](
            ((value >> 8) & 0xff).toByte,
            ((value >> 0) & 0xff).toByte
        )
    }

    protected def serializeNumber(value: Long, insertFlag: Boolean): Array[Byte] = {
        //println(s"Serializing number $value, insertFlag: $insertFlag")

        def flag(array: Array[Byte]): Array[Byte] = if (insertFlag) array else Array()

        if (Byte.MinValue < value && value < Byte.MaxValue) {
            //println("Byte")
            return flag(ByteFlagArray) ++ Array(value.toByte)
        }

        if (Short.MinValue < value && value < Short.MaxValue) {
            //println(s"Short (${value.toShort}) - " + new String(serializeShort(value.toShort)))
            return flag(ShortFlagArray) ++ serializeShort(value.toShort)
        }

        if (Int.MinValue < value && value < Int.MaxValue) {
            //println("Int")
            return flag(IntFlagArray) ++ serializeInt(value.toInt)
        }

        //println("Long")
        flag(LongFlagArray) ++ serializeLong(value)
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
            def casted[T]: T = value.asInstanceOf[T]

            extractedFieldType match {
                case JByte.TYPE => serializeNumber(casted[Byte], false)
                case JShort.TYPE => serializeNumber(casted[Short], false)
                case JInt.TYPE => serializeNumber(casted[Int], false)
                case JLong.TYPE => serializeNumber(casted, false)
                case JFloat.TYPE => serializeInt(JFloat.floatToRawIntBits(casted))
                case JDouble.TYPE => serializeLong(JDouble.doubleToRawLongBits(casted))
                case JBoolean.TYPE => Array(if (casted) 1.toByte else 0.toByte)
                case s: Class[String] if s == classOf[String] => casted[String].getBytes
                case n: Class[JShort] if NumberWrapperClasses.contains(n) => serializeNumber(casted, false)
                case f: Class[JFloat] if f == classOf[JFloat] => serializeInt(JFloat.floatToRawIntBits(casted))
                case d: Class[JDouble] if d == classOf[JDouble] => serializeLong(JDouble.doubleToRawLongBits(casted))
                case b: Class[JBoolean] if b == classOf[JBoolean] => Array(if (casted) 1.toByte else 0.toByte)
                case e: Class[Enum[_]] if EnumClass.isAssignableFrom(e) => serializeType(clazz) ++ casted[Enum[_]].name().getBytes

                case _ => value match {
                    case _: JByte => serializeNumber(casted[Byte], false)
                    case _: JShort => serializeNumber(casted[Short], false)
                    case _: JInt => serializeNumber(casted[Int], false)
                    case _: JLong => serializeNumber(casted, false)
                    case f: JFloat => FloatFlagArray ++ serializeInt(JFloat.floatToRawIntBits(f))
                    case d: JDouble => DoubleFlagArray ++ serializeLong(JDouble.doubleToRawLongBits(d))
                    case b: JBoolean => IntFlagArray ++ Array[Byte](if (b) 1 else 0)

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

    protected def deserializeNumber(bytes: Array[Byte], from: Int, to: Int): Long = {
        if (to - from > JLong.BYTES)
            throw new IllegalArgumentException(s"trying to convert byte seq to long, but the provided byte seq is longer than a long size (${bytes.length})}")

        println(s"Deserializing number in region ${new String(bytes.slice(from, to))}")
        println(s"bytes(from) = ${bytes(from)}")

        var result = 0
        val limit = to.min(bytes.length) - from
        for (i <- 0 until limit) {
            val byteIndex = from + ((i - limit).abs - 1)
            val b = bytes(byteIndex)
            //println(s"b = ${b}")
            //println(s"i = ${i}")
            result |= (0xff & b) << i * 8
        }

        result
    }

    /**
     * @return a tuple of the deserialized number, and the length of the number in the array.
     * */
    protected def deserializeFlaggedNumber(bytes: Array[Byte], index: Int): (Long, Byte) = {
        //println(s"Deserializing number in region ${new String(bytes.slice(index, index + 8)).replace('\r', ' ')}")
        val number: (Long, Byte) = bytes(index) match {
            case ByteFlag => (bytes(index + 1), 2)
            case ShortFlag => (deserializeNumber(bytes, index + 1, index + 3), 3)
            case IntFlag => (deserializeNumber(bytes, index + 1, index + 5), 5)
            case LongFlag => (deserializeNumber(bytes, index + 1, index + 9), 9)
        }
        //println(s"Deserialized number ${number._1}, ${number._1.getClass}")
        number
    }

    private def readSign(bytes: Array[Byte], from: Int, numOfLengths: Int, lastLengthReference: Int): (Array[Int], Int) = {
        val valuesLengths = new Array[Int](numOfLengths)
        var nextIndex = from

        //println(s"Reading sign ${new String(bytes.drop(from)).replace('\r', ' ')}")

        if (bytes(from) == OneFieldFlag)
            return (Array(lastLengthReference), 1)

        //println(s"valuesLengths = ${valuesLengths.mkString("Array(", ", ", ")")}")
        //println(s"nextIndex = ${nextIndex}")
        //println(s"numOfLengths = ${numOfLengths}")
        for (i <- 0 until numOfLengths) {
            //println(s"ITEM : ${i}: ")
            val length = if (i != numOfLengths - 1) {
                val (number, length) = deserializeFlaggedNumber(bytes, nextIndex)
                nextIndex += length
                //println(s"nextIndex = ${nextIndex}")

                number.toInt
            } else {
                //If we hit the last field, we deduct his length by calculating
                //the object length - total referenced lengths sum
                lastLengthReference - valuesLengths.sum
            }
            valuesLengths(i) = length
            //println(s"valuesLengths = ${valuesLengths.mkString("Array(", ", ", ")")}")
        }
        (valuesLengths, nextIndex - from)
    }

    private def deserializeObject(bytes: Array[Byte]): Any = {
        //println(s"Deserializing object ${new String(bytes).replace('\r', ' ')}")

        if (bytes sameElements NullFlagArray)
            return null

        if (isArray(bytes))
            return deserializeArray(bytes)

        val (kindClass, kindClassLength) = deserializeType(bytes)
        val instance = TheUnsafe.allocateInstance(kindClass)

        if (bytes sameElements NoFieldFlagArray) {
            return instance
        }

        val objectLength = bytes.length
        val fields = listSerializableFields(kindClass)
        val fieldsNumbers = fields.length

        //Reading Instance Sign...
        val (valuesLengths, signBytesLength) = readSign(bytes, kindClassLength, fieldsNumbers, objectLength)
        //println(s"valuesLengths = ${valuesLengths.mkString("Array(", ", ", ")")}")
        //println(s"kindClassLength = ${kindClassLength}")
        //println(s"signBytesLength = ${signBytesLength}")

        //Writing values to the empty instance
        var currentValIndex = kindClassLength + signBytesLength
        for (i <- 0 until fieldsNumbers) {
            val field = fields(i)
            val fieldType = field.getType
            val valueLength = valuesLengths(i)
            //println(s"field = ${field}")
            //println(s"currentValIndex = ${currentValIndex}")
            //println(s"valueLength = ${valueLength}")
            val valueBytes = bytes.slice(currentValIndex, currentValIndex + valueLength)
            //println(s"valueBytes = ${new String(valueBytes)}")
            val value = deserializeValue(fieldType, valueBytes)
            //println(s"value = ${value}")
            //println(s"value.getClass = ${value.getClass}")
            setValue(instance, field, value)

            currentValIndex += valueLength
        }
        instance
    }

    private def setValue(instance: Any, field: Field, value: Any): Unit = {
        val fieldOffset = TheUnsafe.objectFieldOffset(field)

        def casted[A]: A = value.asInstanceOf[A]

        val action: (Any, Long) => Unit = field.getType match {
            case JInt.TYPE => TheUnsafe.putInt(_, _, casted[Long].toInt)
            case JByte.TYPE => TheUnsafe.putByte(_, _, casted[Long].toByte)
            case JShort.TYPE => TheUnsafe.putShort(_, _, casted[Long].toShort)
            case JLong.TYPE => TheUnsafe.putLong(_, _, casted[Long])
            case JDouble.TYPE => TheUnsafe.putDouble(_, _, casted)
            case JFloat.TYPE => TheUnsafe.putFloat(_, _, casted)
            case JBoolean.TYPE => TheUnsafe.putBoolean(_, _, casted)
            case _ => TheUnsafe.putObject(_, _, casted)
        }
        action(instance, fieldOffset)
    }

    private def deserializeEnum[T <: Enum[T]](bytes: Array[Byte]): T = {
        val (clazz, length) = deserializeType(bytes)
        val enumName = new String(bytes.drop(length))
        Enum.valueOf(clazz.asInstanceOf[Class[T]], enumName)
    }

    private def deserializeValue[T](@Nullable fieldType: Class[_], bytes: Array[Byte]): Any = {
        //println(s"Deserializing value of type $fieldType, bytes = ${new String(bytes).replace('\r', ' ')}")
        if (bytes sameElements NullFlagArray) {
            null
        } else if (fieldType != null && fieldType.isArray) {
            deserializeArray(bytes)
        } else if (fieldType != null && EnumClass.isAssignableFrom(fieldType)) {
            deserializeEnum(bytes)
        } else {
            /*
            * The deserialization will determine the type of value the bytes contains by deducting it from the field type
            * in which we want put the deserialized value.
            * If the field type is just another kind of predetermined kinds (number or string), the value type will be deducted from the byte array
            * by using a flag type. the inconvenient of this second method is that we gain 1 byte. (following the serialization method)
            * */
            fieldType match {
                case JByte.TYPE => bytes(0): Long
                case JShort.TYPE => deserializeNumber(bytes, 0, 2): Long
                case JInt.TYPE => deserializeNumber(bytes, 0, 4): Long
                case JFloat.TYPE => JFloat.intBitsToFloat(deserializeNumber(bytes, 0, 4).toInt)
                case JDouble.TYPE => JDouble.longBitsToDouble(deserializeNumber(bytes, 0, 8))
                case JBoolean.TYPE => bytes(0) == 1
                case s: Class[String] if s == classOf[String] => new String(bytes)
                case _ => bytes(0) match { //The field kind is not a number or a string, let's suppose his kind base on the first byte flag
                    //Flag length + int length
                    case ByteFlag | ShortFlag | IntFlag | LongFlag => deserializeNumber(bytes, 0, 8)
                    case StringFlag => new String(bytes.drop(1))
                    case FloatFlag => JFloat.intBitsToFloat(deserializeNumber(bytes, 1, 5).toInt)
                    case DoubleFlag => JDouble.longBitsToDouble(deserializeNumber(bytes, 1, 9))
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

    private val ShortArrayFlag: Byte = -100
    private val ByteArrayFlag: Byte = -99
    private val IntArrayFlag: Byte = -98
    private val LongArrayFlag: Byte = -97
    private val AnyArrayFlag: Byte = -96

    private val IntFlag: Byte = -95
    private val ShortFlag: Byte = -94
    private val ByteFlag: Byte = -93
    private val LongFlag: Byte = -92
    private val FloatFlag: Byte = -91
    private val DoubleFlag: Byte = -90

    private val StringFlag: Byte = -89
    private val NullFlag: Byte = -88
    private val NoFieldFlag: Byte = -87
    private val OneFieldFlag: Byte = -86
    private val EnumFlag: Byte = -85

    private val IntFlagArray: Array[Byte] = Array(IntFlag)
    private val ShortFlagArray: Array[Byte] = Array(ShortFlag)
    private val ByteFlagArray: Array[Byte] = Array(ByteFlag)
    private val LongFlagArray: Array[Byte] = Array(LongFlag)
    private val FloatFlagArray: Array[Byte] = Array(FloatFlag)
    private val DoubleFlagArray: Array[Byte] = Array(DoubleFlag)

    private val StringFlagArray: Array[Byte] = Array(StringFlag)
    private val NullFlagArray: Array[Byte] = Array(NullFlag)
    private val NoFieldFlagArray: Array[Byte] = Array(NoFieldFlag)
    private val OneFieldFlagArray: Array[Byte] = Array(OneFieldFlag)
    private val EnumFlagArray: Array[Byte] = Array(EnumFlag)

    private val NumberWrapperClasses: Array[Class[_]] = Array(classOf[JByte], classOf[JShort], classOf[JInt], classOf[JLong])

    private val ArrayFlags: Array[Array[Byte]] = Array(Array(ByteArrayFlag),
        Array(ShortArrayFlag), Array(IntArrayFlag),
        Array(LongArrayFlag), Array(AnyArrayFlag))

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
