package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.behavior.annotation.{BasicInvocationRule, MethodControl}
import fr.linkit.api.gnom.cache.sync.description.SyncStructureDescription
import fr.linkit.api.gnom.network.statics.ClassStaticAccessor
import fr.linkit.engine.internal.utils.ScalaUtils

class SimpleClassStaticAccessor[A <: AnyRef](desc: SyncStructureDescription[A]) extends ClassStaticAccessor[A] {
    //override val behavior: SynchronizedStructureBehavior[A] = _

    private val methods = desc.listMethods().map(m => (m.javaMethod.getName, m.javaMethod)).toMap
    private val fields  = desc.listFields().map(f => (f.javaField.getName, f.javaField)).toMap

    @MethodControl(BasicInvocationRule.ONLY_OWNER)
    override protected def applyDynamicNamed(method: String)(args: (String, Any)*): Any = {
        ???
    }

    @MethodControl(BasicInvocationRule.ONLY_OWNER)
    override protected def applyDynamic(methodName: String)(args: Any*): Any = {
        val javaMethod = methods(methodName)
        javaMethod.invoke(null, args: _*)
    }

    @MethodControl(BasicInvocationRule.ONLY_OWNER)
    override protected def selectDynamic(fieldName: String): Any = {
        val javaField = fields(fieldName)
        ScalaUtils.getValue(null, javaField)
    }

    @MethodControl(BasicInvocationRule.ONLY_OWNER)
    override protected def updateDynamic(fieldName: String)(newValue: Any): Unit = {
        val javaField = fields(fieldName)
        ScalaUtils.setValue(null, javaField, newValue)
    }
}
