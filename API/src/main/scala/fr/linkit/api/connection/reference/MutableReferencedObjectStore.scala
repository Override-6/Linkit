package fr.linkit.api.connection.reference

import fr.linkit.api.connection.network.reference.ReferencedObjectStore

trait MutableReferencedObjectStore extends ReferencedObjectStore {

    def ++=(refs: AnyRef*): this.type

    def ++=(refs: Map[Int, AnyRef]): this.type

    def putAllNotContained(refs: Map[Int, AnyRef]): this.type

    def +=(anyRef: AnyRef): this.type

    def +=(code: Int, anyRef: AnyRef): this.type

    def -=(ref: AnyRef): this.type
}
