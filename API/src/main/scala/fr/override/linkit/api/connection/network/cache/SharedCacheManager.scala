package fr.`override`.linkit.api.connection.network.cache

import scala.reflect.ClassTag

trait SharedCacheManager {

    def post[A <: Serializable](key: Long, value: A): A

    def get[A <: Serializable](key: Long): Option[A]
    def getOrWait[A <: Serializable](key: Long): A
    def apply[A <: Serializable](key: Long): A

    def get[A <: SharedCache : ClassTag](cacheID: Long, factory: SharedCacheFactory[A]): A

}
