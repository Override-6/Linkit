package fr.overridescala.vps.ftp.api.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface relayExtensionInfo {

    String name() default "";

    String[] dependencies() default {};

}
