package fr.linkit.engine.connection.packet.persistence.serializor;

import fr.linkit.api.connection.cache.obj.SynchronizedObject;

public class ConstantProtocol {

    private static byte i = 0;
    public static final byte Class = i++;
    public static final byte SyncClass = i++;
    public static final byte ContextRef = i++;

    public static final byte String = i++;
    public static final byte Int = i++;
    public static final byte Short = i++;
    public static final byte Long = i++;
    public static final byte Byte = i++;
    public static final byte Double = i++;
    public static final byte Float = i++;
    public static final byte Boolean = i++;
    public static final byte Char = i++;

    public static final byte Object = i++;
    public static final byte Array = i++;

    public static final byte ChunkCount = i;

}
