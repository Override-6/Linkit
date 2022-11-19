/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.persistence;

public class ProtocolConstants {

    //increment each time the protocol changes
    public static final short ProtocolVersion = 6;

    public static final byte UByteSize = 1;
    public static final byte UShortSize = 2;
    public static final byte IntSize = 3;

    public static final byte Class = 0;         // chunk mark = 0b0000000000000001
    public static final byte SyncDef = 1;       // chunk mark = 0b0000000000000010
    public static final byte String = 2;        // chunk mark = 0b0000000000000100
    public static final byte Int = 3;           // chunk mark = 0b0000000000001000
    public static final byte Short = 4;         // chunk mark = 0b0000000000010000
    public static final byte Long = 5;          // chunk mark = 0b0000000000100000
    public static final byte Byte = 6;          // chunk mark = 0b0000000001000000
    public static final byte Double = 7;        // chunk mark = 0b0000000010000000
    public static final byte Float = 8;         // chunk mark = 0b0000000100000000
    public static final byte Boolean = 9;       // chunk mark = 0b0000001000000000
    public static final byte Char = 10;         // chunk mark = 0b0000010000000000
    public static final byte Enum = 11;         // chunk mark = 0b0000100000000000
    public static final byte Object = 12;       // chunk mark = 0b0001000000000000
    public static final byte NetworkObject = 13;// chunk mark = 0b0010000000000000
    public static final byte Array = 14;        // chunk mark = 0b0100000000000000
    public static final byte RNO = 15;          // chunk mark = 0b1000000000000000 - Referenced Network Object
    public static final byte ChunkCount = 16;

}
