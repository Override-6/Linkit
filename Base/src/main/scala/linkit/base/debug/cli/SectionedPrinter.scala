/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package linkit.base.debug.cli

import java.io.PrintStream
import java.util
import scala.collection.mutable

class SectionedPrinter(out: PrintStream) {
    type Section = mutable.StringBuilder
    private val sectionStack = new util.IdentityHashMap[Section, InternSection]

    implicit class ExtendedSection(val s: Section) {
        def enable(): s.type = setPrintInStream(true)

        def disable(): s.type = setPrintInStream(false)

        def flush(): Unit = {
            sectionStack.get(s).flush()
        }

        private def setPrintInStream(printInStream: Boolean): s.type = {
            sectionStack.get(s)
                .setPrintInStream(printInStream)
            s
        }
    }

    def newSection(): Section = {
        val section = new Section()
        sectionStack.put(section, new InternSection(section))
        section
    }


    private class InternSection(val section: Section) {
        private var printInStream: Boolean = false

        def setPrintInStream(printInStream: Boolean): Unit = this.printInStream = printInStream

        def flush(): Unit = if (printInStream) {
            val str = section.result()
            out.print(str)
            section.clear()
        }

        def isPrintEnabled: Boolean = printInStream
    }

}
