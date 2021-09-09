package fr.linkit.engine.connection.packet.persistence.serializor;

public class ConstantProtocol {

    public static final byte Class = 0;
    public static final byte ContextRef = 1;

    public static final byte String = 2;
    public static final byte Int = 3;
    public static final byte Short = 4;
    public static final byte Long = 5;
    public static final byte Byte = 6;
    public static final byte Double = 7;
    public static final byte Float = 8;
    public static final byte Boolean = 9;
    public static final byte Char = 10;

    public static final byte Array = 11;
    public static final byte Object = 12;

    public static final byte ChunkCount = 13;

    public static final byte Null = -128;
    public static final byte PoolRef = -127;
}
