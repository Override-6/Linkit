package fr.linkit.engine.internal.debug

import java.io.PrintStream
import java.sql.Timestamp


trait TrafficDump {

    def injections: InjectionsDump

    def requestHistory(from: Timestamp, to: Timestamp): RequestsDump

    def print(out: PrintStream): Unit
}
