package fr.linkit.engine.internal.reflection;

import java.lang.reflect.Constructor;

public class NativeAccess {

    private static final byte OBJECT_FLAG = -1;
    private static final byte BYTE_FLAG = 0;
    private static final byte BOOLEAN_FLAG = 1;
    private static final byte CHAR_FLAG = 2;
    private static final byte SHORT_FLAG = 3;
    private static final byte INT_FLAG = 4;
    private static final byte LONG_FLAG = 5;
    private static final byte FLOAT_FLAG = 6;
    private static final byte DOUBLE_FLAG = 7;

    public static native Object allocate(Class<?> clazz);

    public static void invokeConstructor(Object target, Constructor<?> constructor, Object[] arguments) {
        Class<?>[] params = constructor.getParameterTypes();
        String signature = getConstructorDescriptor(params, Void.TYPE);
        invokeMethod(target, "<init>", signature, params, arguments);
    }


    public static void invokeMethod(Object target, String name, String signature, Class<?>[] params, Object[] arguments) {
        byte[] paramTypes = new byte[params.length];

        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> param = params[i];
            paramTypes[i] = switch (param.getName()) {
                case "byte" -> BYTE_FLAG;
                case "boolean" -> BOOLEAN_FLAG;
                case "char" -> CHAR_FLAG;
                case "short" -> SHORT_FLAG;
                case "int" -> INT_FLAG;
                case "long" -> LONG_FLAG;
                case "float" -> FLOAT_FLAG;
                case "double" -> DOUBLE_FLAG;
                default -> OBJECT_FLAG;
            };
        }
        invokeMethod(target, name, signature, paramTypes, arguments);
    }

    private static String getConstructorDescriptor(Class<?>[] params, Class<?> returnType) {
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

    private static native Object invokeMethod(
            Object o, String name, String signature, byte[] paramTypes, Object[] args
    );


}
