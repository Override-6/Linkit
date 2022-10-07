package fr.linkit.api.internal.concurrency

import fr.linkit.api.internal.system.delegate.ImplementationDelegates

import scala.concurrent.Future

trait Procrastinator {

    def runLater[A](f: => A): Future[A]

}

object Procrastinator {

    private final val supplier = ImplementationDelegates.defaultProcrastinatorSupplier

    private[linkit] trait Supplier {
        def apply(name: String): Procrastinator

        def current: Option[Procrastinator]

        def currentWorker: Option[Worker]

    }

    def currentWorker: Option[Worker] = supplier.currentWorker

    def current: Option[Procrastinator] = supplier.current

    def apply(name: String): Procrastinator = supplier(name)
}