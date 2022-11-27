package fr.linkit.engine.gnom.persistence.serial

import fr.linkit.engine.gnom.persistence.PersistenceException

class UnreleasedComputationLockException(msg: String, cause: Throwable = null) extends PersistenceException(msg, cause) {

}
