package fr.linkit.engine.local.generation.cbp

import fr.linkit.api.local.generation.cbp.ValueScope
import fr.linkit.api.local.generation.{ClassGenerator, CompilerType}

import scala.collection.mutable

abstract class AbstractClassGenerator[T] extends ClassGenerator[T] {

    protected val roots = new mutable.HashMap[CompilerType, ValueScope[T]]()

    override def registerRootScope(compilerType: CompilerType, valueScope: ValueScope[T]): Unit = {
        roots.put(compilerType, valueScope)
    }

}
