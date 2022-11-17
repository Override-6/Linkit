package fr.linkit.api.gnom.network.tag


/**
 * A specific kind of tag that is used to make a specific selection over specific engines (see implementations).
 * */
sealed trait TagSelection[+E <: EngineTag]

final case class Select[+E <: EngineTag](tag: E) extends TagSelection[E] {
    override def toString: String = tag.toString
}

/**
 * Inverses a tag selection.<br>
 * Example: !Server means Clients,
 * <br> !Everyone means Nobody
 * */
final case class Not[+E <: EngineTag](tag: TagSelection[E]) extends TagSelection[E] {
    override def toString: String = s"!$tag"
}

/**
 * Union of all given tags.<br>
 * Example: Server U Clients means Everyone,<br>
 * !(NameTag("client1") U NameTag("client2")) means everyone except client1 and client2
 * */
final case class Union[+E <: EngineTag](a: TagSelection[E], b: TagSelection[E]) extends TagSelection[E] {
    override def toString: String = s"$a U $b"
}

final case class Intersection[+E <: EngineTag](a: TagSelection[E], b: TagSelection[E]) extends TagSelection[E] {
    override def toString: String = s"$a ∩ $b"
}

object TagSelection {



    implicit def toSelect[A <: EngineTag](tag: A): TagSelection[A] = Select(tag)

    implicit class TagOps[A <: EngineTag](tag: A) {

        def unary_!(): Not[A] = Not(tag)

        def -[B >: A](other: B with EngineTag): TagSelection[B with EngineTag] = Intersection(tag, !other)
        def -[B >: A](other: TagSelection[B with EngineTag]): TagSelection[B with EngineTag] = Intersection(tag, !other)

        def U[B >: A](other: B with EngineTag): TagSelection[B with EngineTag] = Union(tag, other)
        def U[B >: A](other: TagSelection[B with EngineTag]): TagSelection[B with EngineTag] = Union(tag, other)

        def I[B >: A](other: B with EngineTag): TagSelection[B with EngineTag] = Intersection(tag, other)
        def I[B >: A](other: TagSelection[B with EngineTag]): TagSelection[B with EngineTag] = Intersection(tag, other)

    }


    //may have errors on IJ but these compiles
    implicit class SelectOps[A <: EngineTag](tag: TagSelection[A]) {
        def unary_!(): Not[A] = Not(tag)

        def -[B >: A](other: B with EngineTag): TagSelection[B with EngineTag] = Intersection(tag, !other)
        def -[B >: A](other: TagSelection[B with EngineTag]): TagSelection[B with EngineTag] = Intersection(tag, !other)
        def U[B >: A](other: B with EngineTag): TagSelection[B with EngineTag] = Union(tag, other)
        def U[B >: A](other: TagSelection[B with EngineTag]): TagSelection[B with EngineTag] = Union(tag, other)
        def I[B >: A](other: B with EngineTag): TagSelection[B with EngineTag] = Intersection(tag, other)
        def I[B >: A](other: TagSelection[B with EngineTag]): TagSelection[B with EngineTag] = Intersection(tag, other)

    }
}