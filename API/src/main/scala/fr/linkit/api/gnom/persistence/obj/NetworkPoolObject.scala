package fr.linkit.api.gnom.persistence.obj

import fr.linkit.api.gnom.referencing.NetworkObjectReference

trait NetworkPoolObject extends PoolObject[AnyRef] {
    val reference: NetworkObjectReference
}
