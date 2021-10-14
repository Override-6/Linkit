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

    public static final byte Class = 0;
    public static final byte SyncClass = 1;

    public static final byte String = 2;
    public static final byte Int = 3;
    public static final byte Short = 4;
    public static final byte Long = 5;
    public static final byte Byte = 6;
    public static final byte Double = 7;
    public static final byte Float = 8;
    public static final byte Boolean = 9;
    public static final byte Char = 10;

    public static final byte Enum = 11;
    public static final byte Object = 12;
    public static final byte Array = 13;
    public static final byte RNO = 14; //Referenced Network Object

    public static final byte ChunkCount = 15;

}
