package fr.linkit.engine.gnom.persistence.serial.read

import fr.linkit.api.gnom.persistence.obj.{PoolObject, ProfilePoolObject, RegistrablePoolObject}

class ReplacedObject(replacementIndex: Int,
                     pool            : DeserializerObjectPool) extends ProfilePoolObject[AnyRef] with RegistrablePoolObject[AnyRef] {


    private var registrable: RegistrablePoolObject[AnyRef] = _


    override def register(): Unit = if (registrable != null) registrable.register()

    override lazy val value: AnyRef = pool.getAny(replacementIndex) match {
        case o: RegistrablePoolObject[AnyRef] =>
            registrable = o
            o.value
        case o: PoolObject[AnyRef]            => o.value
        case o: AnyRef                        => o
    }

    /**
     * an int to identity this object in the object pool
     * */
    override def identity: Int = replacementIndex
}
