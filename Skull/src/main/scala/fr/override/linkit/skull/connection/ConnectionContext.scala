package fr.`override`.linkit.skull.connection

import fr.`override`.linkit.skull.connection.task.TaskScheduler
import fr.`override`.linkit.skull.internal.concurrency.Procrastinator

trait ConnectionContext extends TaskScheduler with Procrastinator {

}
