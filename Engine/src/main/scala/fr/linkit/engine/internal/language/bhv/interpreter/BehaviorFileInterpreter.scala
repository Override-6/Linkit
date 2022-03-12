package fr.linkit.engine.internal.language.bhv.interpreter

import fr.linkit.api.gnom.cache.sync.contract.descriptors.ContractDescriptorData
import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.engine.internal.language.bhv.PropertyClass
import fr.linkit.engine.internal.language.bhv.ast.BehaviorFile
import fr.linkit.engine.internal.language.bhv.compilation.FileIntegratedLambdas

class BehaviorFileInterpreter(ast: BehaviorFile, fileName: String, center: CompilerCenter, propertyClass: PropertyClass) {
    private val lambdas = new FileIntegratedLambdas(fileName, center)
    def getData: ContractDescriptorData = null
}
