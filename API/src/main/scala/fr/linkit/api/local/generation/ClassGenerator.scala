package fr.linkit.api.local.generation

import fr.linkit.api.local.generation.cbp.ValueScope

trait ClassGenerator[T] {

    def registerRootScope(compilerType: CompilerType, valueScope: ValueScope[T])

    def makeRequest(context: T): CompilationRequest

}
