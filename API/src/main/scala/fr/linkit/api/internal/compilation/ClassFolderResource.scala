package fr.linkit.api.internal.compilation

import fr.linkit.api.application.resource.representation.FolderRepresentation

trait ClassFolderResource[C] extends FolderRepresentation {

    def findClass[S <: AnyRef](className: String, loader: ClassLoader): Option[Class[S with C]]
}
