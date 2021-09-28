package fr.linkit.api.gnom.reference

trait MutableReferencedObjectStore extends ReferencedObjectStore {

    def ++=(refs: AnyRef*): this.type

    def ++=(refs: Map[Int, AnyRef]): this.type

    def putAllNotContained(refs: Map[Int, AnyRef]): this.type

    def +=(anyRef: AnyRef): this.type

    def +=(code: Int, anyRef: AnyRef): this.type

    def -=(ref: AnyRef): this.type
}
