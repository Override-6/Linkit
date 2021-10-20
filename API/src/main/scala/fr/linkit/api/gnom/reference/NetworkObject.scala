/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.reference

/**
 * <p>
 *      Network Objects are an essential part of the GNOM System.
 *      Those are objects that can be represented by their reference
 *      ([[NetworkObject#reference]], see [[NetworkObjectReference]]).
 * </p>
 * <p>
 *      A network object is an object that is present on multiple engines.
 *      They are used mainly by the persistence system, thus, network objects are
 *      replaced by their reference object in the ObjectPool.
 *      As a result, its adds packet stream size optimisation, and moreover, adds more possibility
 *      and scalability for engines concerning object transport:
 *      <br>
 *      <ul>
 *          <li>
 *              <p>
 *                  <u>Not serializable objects</u><br>
 *                  The current persistence system is already able to handle classes that are not designed to
 *                  support serialization, and is enough configurable to let the user define how an object should be
 *                  serialised/deserialized, if the default 'field copy/paste' method is not convenient.<br>
 *                  <br> however,
 *                  there is still enough reason why serialization/deserialization of an object can be undesirable.
 *              </p><br>
 *              <p>
 *                  For example, the trait [[fr.linkit.api.application.ApplicationContext]]'s implementation is intended
 *                  to have only one instance in the JVM. Without the GNOM (or `General Network's Objects Management`),
 *                  if the Application object is set into a packet, and then the packet is sent to any engine, the
 *                  the serialization system will handle the Application object as such, and so, the receiver of the packet will end up with
 *                  two applications objects, which is problematic because only one ApplicationContext is intended.
 *              </p><br>
 *              <p>
 *                  The interest of defining an object's type as being a Network Object is not only useful for
 *                  Singleton-like objects. Any object that should not have multiple clones of it on an engine,
 *                  can extends the [[NetworkObject]] trait and then link up with a reference that is handled by a [[NetworkObjectLinker]].
 *              </p>
 *          </li>
 *          <li>
 *               <u>Not serializable objects</u><br>
 *          </li>
 *          <li>Test</li>
 *      </ul>
 *
 * </p>
 * */
trait NetworkObject[R <: NetworkObjectReference] {

    def reference: R

}
