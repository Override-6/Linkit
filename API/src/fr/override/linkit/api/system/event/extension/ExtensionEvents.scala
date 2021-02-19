package fr.`override`.linkit.api.system.event.extension

import fr.`override`.linkit.api.`extension`.fragment.{ExtensionFragment, RemoteFragment}
import fr.`override`.linkit.api.`extension`.{RelayExtension, RelayExtensionLoader, RelayProperties}
import fr.`override`.linkit.api.extension.LoadPhase
import org.jetbrains.annotations.Nullable

object ExtensionEvents {

    case class ExtensionsStateEvent(extensions: Array[RelayExtension],
                                    exceptions: Array[(RelayExtension, Throwable)],
                                    state: LoadPhase) extends ExtensionEvent {

        override def notifyListener(listener: ExtensionEventListener): Unit = {
            listener.onExtensionsStateChange(this)
            state match {
                case LoadPhase.LOAD => listener.onExtensionsLoad(this)
                case LoadPhase.ENABLE => listener.onExtensionsEnable(this)
                case LoadPhase.DISABLE => listener.onExtensionsDisable(this)
            }
        }
    }

    case class FragmentEvent(fragment: ExtensionFragment,
                             exception: Option[Throwable],
                             private val isEnabledEvent: Boolean) extends ExtensionEvent {
        override def notifyListener(listener: ExtensionEventListener): Unit = {
            if (isEnabledEvent)
                listener.onFragmentEnabled(this)
            else
                listener.onFragmentDestroyed(this)
        }
    }

    case class RemoteFragmentEvent(fragment: RemoteFragment,
                                   exception: Option[Throwable],
                                   private val isEnabledEvent: Boolean) extends ExtensionEvent {
        override def notifyListener(listener: ExtensionEventListener): Unit = {
            if (isEnabledEvent)
                listener.onRemoteFragmentEnable(this)
            else
                listener.onRemoteFragmentDestroy(this)
        }
    }

    case class LoaderPhaseChangeEvent(extensionsLoader: RelayExtensionLoader,
                                      newPhase: LoadPhase, oldPhase: LoadPhase) extends ExtensionEvent {
        override def notifyListener(listener: ExtensionEventListener): Unit = {
            listener.onLoaderPhaseChange(this)
        }
    }

    case class RelayPropertyChangeEvent(properties: RelayProperties,
                                        name: String,
                                        @Nullable newValue: Any, @Nullable oldValue: Any) extends ExtensionEvent {
        override def notifyListener(listener: ExtensionEventListener): Unit = {
            listener.onPropertyChange(this)
        }
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
