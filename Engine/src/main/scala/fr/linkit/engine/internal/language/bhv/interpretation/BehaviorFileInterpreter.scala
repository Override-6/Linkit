package fr.linkit.engine.internal.language.bhv.interpretation

import fr.linkit.api.gnom.cache.sync.contract.descriptors.ContractDescriptorData
import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.engine.internal.language.bhv.PropertyClass
import fr.linkit.engine.internal.language.bhv.ast.BehaviorFile

class BehaviorFileInterpreter(ast: BehaviorFile, fileName: String, center: CompilerCenter, propertyClass: PropertyClass) {
    def getData: ContractDescriptorData = null
}
