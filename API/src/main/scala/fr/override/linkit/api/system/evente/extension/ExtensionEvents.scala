package fr.`override`.linkit.api.system.evente.extension

import fr.`override`.linkit.api.extension.fragment.{ExtensionFragment, RemoteFragment}
import fr.`override`.linkit.api.extension.{LoadPhase, RelayExtension, RelayExtensionLoader, RelayProperties}
import org.jetbrains.annotations.Nullable

object ExtensionEvents {

    case class ExtensionsStateEvent(extensions: Array[RelayExtension],
                                    exceptions: Array[(RelayExtension, Throwable)],
                                    state: LoadPhase) extends ExtensionEvent {


        override def getHooks(category: ExtensionEventHooks): Array[ExtensionEventHook] = {
            val phaseHook = state match {
                case LoadPhase.LOADING => category.extensionsLoad
                case LoadPhase.ENABLING => category.extensionsEnable
                case LoadPhase.DISABLING => category.extensionsDisable
            }
            Array(phaseHook, category.extensionsStateChange)
        }
    }

    case class FragmentEvent(fragment: ExtensionFragment,
                             exception: Option[Throwable],
                             private val isEnabledEvent: Boolean) extends ExtensionEvent {
        override def getHooks(category: ExtensionEventHooks): Array[ExtensionEventHook] = {
            if (isEnabledEvent)
                Array(category.fragmentEnabled)
            else
                Array(category.fragmentDestroyed)
        }
    }

    case class RemoteFragmentEvent(fragment: RemoteFragment,
                                   exception: Option[Throwable],
                                   private val isEnabledEvent: Boolean) extends ExtensionEvent {
        override def getHooks(category: ExtensionEventHooks): Array[ExtensionEventHook] = {
            if (isEnabledEvent)
                Array(category.remoteFragmentEnable)
            else
                Array(category.remoteFragmentDestroy)
        }
    }

    case class LoaderPhaseChangeEvent(extensionsLoader: RelayExtensionLoader,
                                      newPhase: LoadPhase, oldPhase: LoadPhase) extends ExtensionEvent {
        override def getHooks(category: ExtensionEventHooks): Array[ExtensionEventHook] = Array(category.loaderPhaseChange)
    }

    case class RelayPropertyChangeEvent(properties: RelayProperties,
                                        name: String,
                                        @Nullable newValue: Any, @Nullable oldValue: Any) extends ExtensionEvent {
        override def getHooks(category: ExtensionEventHooks): Array[ExtensionEventHook] = Array(category.propertyChange)
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
