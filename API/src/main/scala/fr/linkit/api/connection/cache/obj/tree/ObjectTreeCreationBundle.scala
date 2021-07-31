package fr.linkit.api.connection.cache.obj.tree

import fr.linkit.api.connection.cache.obj.description.ObjectTreeBehavior

case class ObjectTreeCreationBundle[A](puppet: A,
                                       treeIdentifier: Int,
                                       treeOwner: String,
                                       wrappersBehavior: ObjectTreeBehavior)