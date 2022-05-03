/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.persistence.serializor;

public class ConstantProtocol {

    //increment each time the write protocol changes
    public static final short ProtocolVersion = 2;

    public static final byte Class = 0;     // announcement mark = 0b00000000000000001
    public static final byte SyncDef = 1; // announcement mark = 0b00000000000000010

    public static final byte String = 2;    // announcement mark = 0b00000000000000100
    public static final byte Int = 3;       // announcement mark = 0b00000000000001000
    public static final byte Short = 4;     // announcement mark = 0b00000000000010000
    public static final byte Long = 5;      // announcement mark = 0b00000000000100000
    public static final byte Byte = 6;      // announcement mark = 0b00000000001000000
    public static final byte Double = 7;    // announcement mark = 0b00000000010000000
    public static final byte Float = 8;     // announcement mark = 0b00000000100000000
    public static final byte Boolean = 9;   // announcement mark = 0b00000001000000000
    public static final byte Char = 10;     // announcement mark = 0b00000010000000000

    public static final byte Enum = 11;     // announcement mark = 0b00000100000000000
    public static final byte Object = 12;   // announcement mark = 0b00001000000000000
    public static final byte Lambda = 13;   // announcement mark = 0b00010000000000000
    public static final byte Array = 14;    // announcement mark = 0b00100000000000000
    public static final byte RNO = 15;      // announcement mark = 0b01000000000000000 - Referenced Network Object
    public static final byte Mirroring = 16;// announcement mark = 0b10000000000000000

    public static final byte ChunkCount = 17;

}
