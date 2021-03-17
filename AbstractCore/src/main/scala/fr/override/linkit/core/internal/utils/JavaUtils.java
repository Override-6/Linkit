package fr.override.linkit.core.internal.utils;

import java.io.PrintStream;

public class JavaUtils {

    private JavaUtils() {
        //no instance
    }

    public static void printStackTrace(StackTraceElement[] trace, PrintStream out) {
        for (StackTraceElement traceElement : trace)
            out.println("\tat " + traceElement);
    }

}
