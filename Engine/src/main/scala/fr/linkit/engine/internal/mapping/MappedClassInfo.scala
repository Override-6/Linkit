package fr.linkit.engine.internal.mapping

case class MappedClassInfo private(className: String,
                                   classCode: Int,
                                   superClass: MappedClassInfo,
                                   interfaces: Array[MappedClassInfo])
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
