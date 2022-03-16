package fr.linkit.engine.internal.language.bhv

import fr.linkit.api.gnom.reference.linker.GeneralNetworkObjectLinker

abstract class GNOMObjectsProperty(gnol: GeneralNetworkObjectLinker) extends PropertyClass {
    override def get(refName: String): AnyRef = {
        gnol.findObject()
    }

}
