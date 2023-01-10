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

package linkit.base.manipulation;

public class ClassTypeTranslator {
    private static final byte VOID_FLAG = -2;
    private static final byte OBJECT_FLAG = -1;
    private static final byte BYTE_FLAG = 0;
    private static final byte BOOLEAN_FLAG = 1;
    private static final byte CHAR_FLAG = 2;
    private static final byte SHORT_FLAG = 3;
    private static final byte INT_FLAG = 4;
    private static final byte LONG_FLAG = 5;
    private static final byte FLOAT_FLAG = 6;
    private static final byte DOUBLE_FLAG = 7;

    public static byte determineType(Class<?> clazz) {
        return switch (clazz.getName()) {
            case "byte" -> BYTE_FLAG;
            case "boolean" -> BOOLEAN_FLAG;
            case "char" -> CHAR_FLAG;
            case "short" -> SHORT_FLAG;
            case "int" -> INT_FLAG;
            case "long" -> LONG_FLAG;
            case "float" -> FLOAT_FLAG;
            case "double" -> DOUBLE_FLAG;
            case "void" -> VOID_FLAG;
            default -> OBJECT_FLAG;
        };
    }

}
