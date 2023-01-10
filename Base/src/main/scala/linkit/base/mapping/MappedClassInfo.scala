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

package linkit.base.mapping

case class MappedClassInfo private(className: String,
                                   classCode: Int,
                                   superClass: MappedClassInfo,
                                   interfaces: Array[MappedClassInfo]) {

    def extendsFrom(className: String): Boolean = {
        extendsFrom(className.hashCode)
    }

    def extendsFrom(classCode: Int): Boolean = {
        superClass.classCode == classCode || interfaces.exists(_.classCode == classCode) ||
                superClass.extendsFrom(classCode) || interfaces.exists(_.extendsFrom(classCode))
    }

    def clazz: Class[_] = ClassMappings.getClass(classCode)

}

object MappedClassInfo {

    private final val ObjectClassName = classOf[Object].getName
    final         val Object          = MappedClassInfo(ObjectClassName, ObjectClassName.hashCode, null, Array.empty)

    def apply(clazz: Class[_]): MappedClassInfo = {
        val name       = clazz.getName
        val code       = name.hashCode
        val superClass = ClassMappings.getClassInfo(clazz.getSuperclass)
        val interfaces = clazz.getInterfaces.map(ClassMappings.getClassInfo)
        MappedClassInfo(name, code, superClass, interfaces)
    }
}
