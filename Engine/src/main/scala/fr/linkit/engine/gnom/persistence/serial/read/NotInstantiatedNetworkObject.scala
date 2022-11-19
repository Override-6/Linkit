package fr.linkit.engine.gnom.persistence.serial.read

import fr.linkit.api.gnom.persistence.obj.{PoolObject, ProfilePoolObject, RegistrablePoolObject}
import fr.linkit.api.gnom.referencing.NetworkObjectReference
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.persistence.obj.NetworkObjectReferencesLocks

class NotInstantiatedNetworkObject[T <: AnyRef](referenceIdx: Int,
                                                pool        : DeserializerObjectPool,
                                                obj         : ProfilePoolObject[T] with RegistrablePoolObject[T])
        extends ProfilePoolObject[T] with RegistrablePoolObject[T] {

    private lazy val reference = pool.getAny(referenceIdx) match {
        case o: PoolObject[NetworkObjectReference] => o.value
        case o: NetworkObjectReference             => o
    }

    override def value: T = {
        val lock = NetworkObjectReferencesLocks.getInitializationLock(reference)
        AppLoggers.Persistence.debug(s"DESERIALIZING NETWORK OBJECT $reference")
        lock.lock() //lock from start of deserialization...
        val v = try obj.value
        catch {
            case e: Throwable =>
                lock.unlock()
                throw e
        }
        AppLoggers.Persistence.debug(s"DESERIALIZED NETWORK OBJECT $reference")
        v
    }

    override def register(): Unit = {
        val lock = NetworkObjectReferencesLocks.getInitializationLock(reference)
        try obj.register()
        finally lock.release() //...to end of registration
    }

    override def identity: Int = obj.identity
}
