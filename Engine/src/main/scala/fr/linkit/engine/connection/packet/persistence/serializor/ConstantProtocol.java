package fr.linkit.engine.connection.packet.persistence.serializor;

public class ConstantProtocol {

    public static final byte Class = 0; //0
    public static final byte SyncClass = 1;
    public static final byte ContextRef = 2; //2

    public static final byte String = 3; //3
    public static final byte Int = 4;
    public static final byte Short = 5;
    public static final byte Long = 6;
    public static final byte Byte = 7; //7
    public static final byte Double = 8;
    public static final byte Float = 9;
    public static final byte Boolean = 10; //10
    public static final byte Char = 11; //11

    public static final byte Enum = 12; //12
    public static final byte Object = 13; //13
    public static final byte Array = 14;

    public static final byte ChunkCount = 15;

}
