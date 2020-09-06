package fr.overridescala.vps.ftp.api.tests

import scala.reflect.io.Path

class A[T] {



}
class B extends A[Path] {

}

class C {

    val test: A[Path] = new B()

}