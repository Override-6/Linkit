package fr.overridescala.vps.ftp.api.`extension`

import scala.collection.mutable

class RelayProperties {

    private val map: mutable.Map[String, Object] = mutable.Map.empty

    def putProperty(key: String, value: Object): Unit = {
        map.put(key, value)
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

}
