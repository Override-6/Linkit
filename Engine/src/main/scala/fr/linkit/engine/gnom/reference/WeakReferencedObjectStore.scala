package fr.linkit.engine.gnom.reference

import java.lang.ref.{Reference, ReferenceQueue, WeakReference}
import java.util
import java.util.Map.Entry

import fr.linkit.api.gnom.reference.MutableReferencedObjectStore

class WeakReferencedObjectStore(parent: WeakReferencedObjectStore) extends MutableReferencedObjectStore {

    private val codeToRef = new util.HashMap[Int, WeakReference[AnyRef]]()
    private val refToCode = new util.WeakHashMap[AnyRef, Int]()

    override def isPresent(l: Int): Boolean = {
        (parent != null && parent.isPresent(l)) || codeToRef.containsKey(l)
    }

    override def findLocation(ref: AnyRef): Option[Int] = {
        val found = refToCode.get(ref)
        if ((found == null) && parent != null)
            return parent.findLocation(ref)
        Option(found)
    }

    override def findObject(location: ReferencedObjectLocation): Option[AnyRef] = {
        val code = location.refCode
        val found = codeToRef.get(code)
        if (found == null && parent != null)
            return parent.findObject(location)
        Option(found)
    }

    override def ++=(refs: Map[Int, AnyRef]): this.type = {
        refs.foreachEntry((id, ref) => +=(id, ref))
        this
    }

    override def putAllNotContained(refs: Map[Int, AnyRef]): this.type = {
        ++=(refs.filterNot(p => refToCode.containsKey(p._2)))
    }

    override def ++=(refs: AnyRef*): this.type = {
        refs.foreach(+=)
        this
    }

    def ++=(other: WeakReferencedObjectStore): this.type = {
        ++=(other.codeToRef
            .entrySet()
            .toArray(new Array[Entry[Int, WeakReference[AnyRef]]](_))
            .map(p => (p.getKey, p.getValue.get()))
            .toMap)
        this
    }

    override def +=(anyRef: AnyRef): this.type = {
        if (anyRef eq null)
            -=(anyRef)
        else
            +=(anyRef.hashCode(), anyRef)
    }

    override def +=(code: Int, anyRef: AnyRef): this.type = {
        if (refToCode.put(anyRef, code) == null) {
            throw new ObjectAlreadyReferencedException(s"Object $anyRef is already referenced with identifier '${codeToRef.get(code)}'.")
        }
        codeToRef.put(code, newWeakReference(anyRef))
        this
    }

    private def newWeakReference(ref: AnyRef): WeakReference[AnyRef] = {
        new WeakReference[AnyRef](ref, eventQueue)
    }

    override def -=(ref: AnyRef): this.type = {
        val code = refToCode.remove(ref)
        if (code == null) {
            codeToRef.remove(code)
        }
        this
    }

    private val eventQueue = new ReferenceQueue[AnyRef]() {
        override def poll(): Reference[_ <: AnyRef] = {
            val code = refToCode.get(super.poll())
            codeToRef.remove(code)
        }
    }

}
