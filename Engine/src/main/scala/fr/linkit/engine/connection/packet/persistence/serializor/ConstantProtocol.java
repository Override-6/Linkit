package fr.linkit.engine.connection.packet.persistence.serializor;

public class ConstantProtocol {

    private static byte i = 0;
    public static final byte Class = i++;
    public static final byte SyncClass = i++;
    public static final byte ContextRef = i++; //2

    public static final byte String = i++; //3
    public static final byte Int = i++;
    public static final byte Short = i++;
    public static final byte Long = i++;
    public static final byte Byte = i++; //7
    public static final byte Double = i++;
    public static final byte Float = i++;
    public static final byte Boolean = i++; //10
    public static final byte Char = i++; //11

    public static final byte Enum = i++; //12
    public static final byte Object = i++; //13
    public static final byte Array = i++;

    public static final byte ChunkCount = i;

}
