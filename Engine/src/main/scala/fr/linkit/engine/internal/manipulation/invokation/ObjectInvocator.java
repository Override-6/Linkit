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

package fr.linkit.engine.internal.manipulation.invokation;

public class ObjectInvocator {
    
    private static String getMethodDescriptor(Class<?>[] params, Class<?> returnType) {
        var sb = new StringBuilder("(");
        for (Class<?> param : params) {
            sb.append(typeStringClass(param));
        }
        sb.append(')').append(typeStringClass(returnType));
        return sb.toString();
    }

    private static String typeStringClass(Class<?> clazz) {
        if (clazz == Void.TYPE)
            return "V";
        String arrayString = java.lang.reflect.Array.newInstance(clazz, 0).toString();
        return arrayString.substring(1, arrayString.indexOf('@')).replace(".", "/");
    }

    static native Object invokeMethod0(
            Object o, String name, String signature,
            byte[] paramTypes, byte returnType,
            Object[] args
    );


}
