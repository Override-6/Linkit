package fr.`override`.linkit.api.system.event

import jdk.jfr.{FlightRecorder, FlightRecorderListener}
import jdk.jfr.internal.{JVM, JVMSupport, JVMUpcalls, LogTag}
import jdk.nashorn.internal.codegen.Label
import sun.tracing.dtrace

trait EventHook[L <: EventListener, E <: Event[L]] {

    def await(lock: AnyRef = this): Unit //Would wait until the hooked event triggers

    def add(action: E => Unit): Unit //would add an action to execute every times the event fires
    def add(action: => Unit): Unit

    def addOnce(action: E => Unit): Unit //would add an action to execute every times the event fires
    def addOnce(action: => Unit): Unit

    def cancel(): Unit //Would cancel this hook (don't execute anything, stop waiting if awaited)

    def executeEvent(event: E, listeners: Seq[L]): Unit

    class test extends jdk.jfr.Event {
       new jdk.internal.org.objectweb.asm.MethodVisitor(3).visitTryCatchBlock(Label.)
    }
}
