package fr.`override`.linkit.core.internal.plugin

import scala.collection.mutable

class RelayProperties {

    private val map: mutable.Map[String, Any] = mutable.Map.empty

    def putProperty(key: String, value: Any): Any = {
        map.put(key, value).orNull
    }

    def get[T](key: String): Option[T] = {
        map.get(key).asInstanceOf[Option[T]]
    }

    def getProperty[T](key: String): T = {
        val opt = get(key)
        if (opt.isEmpty)
            throw new NoSuchElementException(s"value '$key' is not set into this relay properties")
        opt.get
    }

    def foreach(action: (String, Any) => Unit): Unit = {
        map.foreach(element => action(element._1, element._2))
    }

}
