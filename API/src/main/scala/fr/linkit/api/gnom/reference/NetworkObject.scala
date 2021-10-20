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
 *      and abstraction for engines concerning objects transport:
 * </p>
 *      <br>
 *      <ul>
 *          <li>
 *              <p>
 *                  <u>Not serializable objects</u><br>
 *                  The current persistence system is already able to handle objects that are not designed to
 *                  support serialization, and is enough configurable to let the user define how an object should be
 *                  serialised/deserialized if the default 'field copy/paste' method is not convenient.<br>
 *                  however,
 *                  there is still enough reason why serialization/deserialization of an object can be undesirable.
 *              </p><br>
 *              <p>
 *                  For example, the trait [[fr.linkit.api.application.ApplicationContext]]'s implementations objects
 *                  are just too big to be sent threw the network. Moreover, they are intended
 *                  to have only one instance in the JVM. Without the GNOM (`General Network's Objects Management`),
 *                  if the Application object is set into a packet, and then the packet is sent to any engine, the
 *                  the serialization system will handle the Application object as such, and so, the receiver of the packet will end up with
 *                  two applications objects, which is problematic because only one ApplicationContext is intended.<br>
 *                  Thanks to the GNOM System, this kind of conceptual problem can be avoided as only the application's reference
 *                  will be sent in the socket, and then the receiver will replace the reference by tis Application object.
 *              </p><br>
 *          </li>
 *          <li>
 *              <p>
 *               <u>Relative Objects</u><br>
 *                  Here, understanding what is a Network Object Reference is important to understand the following reasoning.
 *                  So please, take a look at [[NetworkObjectReference]] documentation to make a fast idea of what is network objects referencing.
 *               </p>
 *               <p>
 *                  Well, as NetworkObjects are bound to their reference, this means that a Network Object's Reference (or NOR) can point
 *                  to a network object which can be different depending on the machine. <br>
 *                  As an example, let's assume that we are making a multiplayer game.<br>
 *                  For this game, we define a trait `Player extends NetworkObject[PlayerReference]` and two implementations of `Player`:
 *                  <dl>
 *                      <dd>
 *                          - ControlledPlayer, relative to engines,
 *                          which is the player object of the human behind the screen that can actually move the player
 *                      </dd>
 *                      <dd>
 *                          - RemotePlayer, for remote players that are connected to our session.<br>
 *                      </dd>
 *                  </dl>
 *               </p>
 *               <p>
 *                  We can define a reference (such as `@session/players/controller`) that all ControlledPlayer objects of the network
 *                  will be bound to (invoking ControlledPlayer.reference will return `@session/players/controller`). Like this, once
 *                  the object is sent to an engine, as Network Object are replaced by their reference during the serialization,
 *                  the engine that receive the reference will take the object referenced at @session/players/controller,
 *                  which will result to its own ControlledPlayer instance.<br>
 *               </p><br>
 *               <p>
 *                  Conversely, we could define a reference to each player connected on the session.
 *                  For engine 'n', we would have `@session/players/n`, and then, we bind our ControlledPlayer object
 *                  to the network object reference where 'n' is equals to our actual engine's identifier.
 *                  This way, when we receive a player object, we are sure that it will be the correct type, and the
 *                  conversion from the 'RemotePlayer' object of the engine that sends us our own player instance will be naturally done.
 *               </p>
 *          </li>
 *          <li><u>Keeping references</u><br>
 *              <p>
 *                  Note: Do not be confused between <b>Network</b> Object Reference and a normal reference.
 *                  Object Reference, or 'reference' is simply the normal reference of an object,
 *                  the usual term to qualify an instance (variables, etc)
 *                  Network Object References are, as said multiple times, the reference of an object in the network.
 *              </p><br>
 *              <p>
 *                  Using normal objects, if you send twice the same object to an engine, the engine will get two different
 *                  clones of the object you sent.<br>
 *                  Using NetworkObject, ensures that the object will have only one reference of itself on engines, without having any undesirable clones.
 *                  Imagine that you want to send a mutable object to an engine, you probably do not know
 *                  what does the engine will do with the object, maybe it would get modified, or registered anywhere,
 *                  but you, you just want to share the object to the engine / inform that the engine have something to do with
 *                  the network object.<br>
 *              </p>
 *          </li>
 *      </ul>
 *  <p>
 *      Using [[fr.linkit.api.gnom.persistence.context.PersistenceConfig]] and [[ContextObjectLinker]], it's possible to
 *      dynamically bind any object to a network reference. However, the user is only free to set an integer as an identifier for the object.
 *      The resulting network reference is a [[fr.linkit.api.gnom.persistence.context.ContextualObjectReference]].<br>
 *      Binding an object to a network reference is sufficient for the object to be handled as a network object by the persistence system.
 *  <p>
 *  @tparam R the type of reference of the network object
 * */
trait NetworkObject[R <: NetworkObjectReference] {

    /**
     * The reference of this Network Object.
     * @see [[NetworkObjectReference]]
     * */
    def reference: R

}
