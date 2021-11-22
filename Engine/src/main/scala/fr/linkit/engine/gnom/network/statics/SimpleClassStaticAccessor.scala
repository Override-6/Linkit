package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.{BasicInvocationRule, MethodControl}
import fr.linkit.api.gnom.cache.sync.contract.description.SyncStructureDescription
import fr.linkit.api.gnom.network.statics.ClassStaticAccessor
import fr.linkit.api.gnom.persistence.context.Deconstructible
import fr.linkit.engine.gnom.persistence.context.Persist
import fr.linkit.engine.gnom.persistence.context.structure.ArrayObjectStructure
import fr.linkit.engine.internal.utils.ScalaUtils

class SimpleClassStaticAccessor[A <: AnyRef]@Persist() (desc: SyncStructureDescription[A]) extends ClassStaticAccessor[A] with Deconstructible {
    //override val behavior: SynchronizedStructureBehavior[A] = _

    private val methods = desc.listMethods()
            .map(_.javaMethod)
            .groupBy(_.getName)
            .map(m => (m._1, m._2.map(m => (ArrayObjectStructure(m.getParameterTypes: _*), m))))
    private val fields  = desc.listFields().map(f => (f.javaField.getName, f.javaField)).toMap

    @MethodControl(BasicInvocationRule.ONLY_OWNER)
    override def applyDynamic[T](methodName: String)(args: Any*): T = {
        val (_, javaMethod) = methods(methodName).find(_._1.isAssignable(args.toArray)).get
        val result = javaMethod.invoke(null, args: _*).asInstanceOf[T]
        result
    }

    @MethodControl(BasicInvocationRule.ONLY_OWNER)
    override def selectDynamic[T](fieldName: String): T = {
        val javaField = fields(fieldName)
        ScalaUtils.getValue(null, javaField).asInstanceOf[T]
    }

    @MethodControl(BasicInvocationRule.ONLY_OWNER)
    override def updateDynamic(fieldName: String)(newValue: Any): Unit = {
        val javaField = fields(fieldName)
        ScalaUtils.setValue(null, javaField, newValue)
    }

    override def deconstruct(): Array[Any] = Array(desc)
}