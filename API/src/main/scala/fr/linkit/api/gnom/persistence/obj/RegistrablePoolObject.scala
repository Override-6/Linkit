package fr.linkit.api.gnom.persistence.obj

trait RegistrablePoolObject[T <: AnyRef] extends PoolObject[T]{

    def register(): Unit

}
