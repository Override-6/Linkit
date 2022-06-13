package fr.linkit.engine.internal.language.bhv.interpreter

import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData

trait LangContractDescriptorData extends ContractDescriptorData {
    
    /**
     * The language source content
     * */
    val filePath      : String
    val fileName      : String
    /**
     * property class that goes with the language source.
     * */
    val propertiesName: String
}