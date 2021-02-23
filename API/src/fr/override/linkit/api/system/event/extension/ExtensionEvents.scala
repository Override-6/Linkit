package fr.`override`.linkit.api.system.event.extension

import fr.`override`.linkit.api.`extension`.fragment.{ExtensionFragment, RemoteFragment}
import fr.`override`.linkit.api.`extension`.{RelayExtension, RelayExtensionLoader, RelayProperties}
import fr.`override`.linkit.api.extension.LoadPhase
import fr.`override`.linkit.api.system.event.EventHook
import org.jetbrains.annotations.Nullable

object ExtensionEvents {

    import ExtensionEventHooks._

    case class ExtensionsStateEvent(extensions: Array[RelayExtension],
                                    exceptions: Array[(RelayExtension, Throwable)],
                                    state: LoadPhase) extends ExtensionEvent {


        override def getHooks: Array[EventHook[ExtensionEventListener, this.type]] = {
            val phaseHook = state match {
                case LoadPhase.LOAD => ExtensionsLoad
                case LoadPhase.ENABLE => ExtensionsEnable
                case LoadPhase.DISABLE => ExtensionsDisable
            }
            Array(phaseHook, ExtensionsStateChange)
        }
    }

    case class FragmentEvent(fragment: ExtensionFragment,
                             exception: Option[Throwable],
                             private val isEnabledEvent: Boolean) extends ExtensionEvent {
        override def getHooks: Array[EventHook[ExtensionEventListener, this.type]] = {
            if (isEnabledEvent)
                Array(FragmentEnabled)
            else
                Array(FragmentDestroyed)
        }
    }

    case class RemoteFragmentEvent(fragment: RemoteFragment,
                                   exception: Option[Throwable],
                                   private val isEnabledEvent: Boolean) extends ExtensionEvent {
        override def getHooks: Array[EventHook[ExtensionEventListener, this.type]] = {
            if (isEnabledEvent)
                Array(RemoteFragmentEnable)
            else
                Array(RemoteFragmentDestroy)
        }
    }

    case class LoaderPhaseChangeEvent(extensionsLoader: RelayExtensionLoader,
                                      newPhase: LoadPhase, oldPhase: LoadPhase) extends ExtensionEvent {
        override def getHooks: Array[EventHook[ExtensionEventListener, this.type]] = Array(LoaderPhaseChange)
    }

    case class RelayPropertyChangeEvent(properties: RelayProperties,
                                        name: String,
                                        @Nullable newValue: Any, @Nullable oldValue: Any) extends ExtensionEvent {
        override def getHooks: Array[EventHook[ExtensionEventListener, this.type]] = Array(PropertyChange)
    }

    def extensionsState(extensions: Array[RelayExtension],
                        exceptions: Array[(RelayExtension, Throwable)],
                        state: LoadPhase): ExtensionsStateEvent = ExtensionsStateEvent(extensions, exceptions, state)

    def fragmentEnable(fragment: ExtensionFragment,
                       @Nullable exception: Throwable): ExtensionEvent = {
        fragment match {
            case remote: RemoteFragment => RemoteFragmentEvent(remote, Option(exception), true)
            case _ => FragmentEvent(fragment, Option(exception), true)
        }
    }

    def fragmentDisable(fragment: ExtensionFragment,
                        @Nullable exception: Throwable): ExtensionEvent = {
        fragment match {
            case remote: RemoteFragment => RemoteFragmentEvent(remote, Option(exception), false)
            case _ => FragmentEvent(fragment, Option(exception), false)
        }
    }

    def loaderPhaseTransition(loader: RelayExtensionLoader, newPhase: LoadPhase, oldPhase: LoadPhase): Unit = {
        LoaderPhaseChangeEvent(loader, newPhase, oldPhase)
    }

    def relayPropertyChange(properties: RelayProperties,
                            name: String,
                            @Nullable newValue: Any, @Nullable oldValue: Any): Unit = {
        RelayPropertyChangeEvent(properties, name, newValue, oldValue)
    }


}
