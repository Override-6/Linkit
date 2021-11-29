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

package fr.linkit.engine.internal.manipulation.creation;

public class ObjectCreator {

    public static native Object allocate(Class<?> clazz);

    public static void pasteAllFields(Object target, String[] fieldNames, String[] fieldSignatures, Object[] fieldValues) {
        if (fieldNames.length != fieldSignatures.length || fieldNames.length != fieldValues.length) {
            throw new IllegalArgumentException("Field names, field types and field values arrays must have the same length.");
        }
        pasteAllFields0(target, fieldNames, fieldSignatures, fieldValues);
    }

    private static native void pasteAllFields0(Object target, String[] fieldNames, String[] fieldSignatures, Object[] fieldValues);

}
