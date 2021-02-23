package fr.`override`.linkit.api.system.event.extension

import com.sun.java.util.jar.pack
import fr.`override`.linkit.api.system.event.EventListener
import fr.`override`.linkit.api.system.event.extension.ExtensionEvents._
import jdk.internal.org.objectweb.asm.{ClassReader, ClassWriter}
import jdk.nashorn.internal.codegen
import jdk.nashorn.internal.codegen.DumpBytecode
import jdk.nashorn.internal.ir.Block

abstract class ExtensionEventListener extends EventListener {

    def onExtensionsLoad(event: ExtensionsStateEvent): Unit = ()

    def onExtensionsEnable(event: ExtensionsStateEvent): Unit = ()

    def onExtensionsDisable(event: ExtensionsStateEvent): Unit = ()

    def onExtensionsStateChange(event: ExtensionsStateEvent): Unit = ()

    def onFragmentEnabled(event: FragmentEvent): Unit = ()

    def onFragmentDestroyed(event: FragmentEvent): Unit = ()

    def onRemoteFragmentEnable(event: RemoteFragmentEvent): Unit = ()

    def onRemoteFragmentDestroy(event: RemoteFragmentEvent): Unit = ()

    def onLoaderPhaseChange(event: LoaderPhaseChangeEvent): Unit = ()

    def onPropertyChange(event: RelayPropertyChangeEvent): Unit = ()

}
