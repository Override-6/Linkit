package fr.linkit.api.gnom.persistence

import fr.linkit.api.gnom.network.tag.Current

import scala.annotation.StaticAnnotation

/**
 * Used to inform that the annotated type can be handled specifically by the persistence system and thus
 * an object of the annotated type have a chance to get changed by an instance of its upper types (e.g interface / mother class). <br>
 * Thus, the user may never declare a parameter or variable that is directly of the annotated type. <br>
 * Example: [[Current]] is annotated with @[[unstabletype]],
 * according to its documentation it serializes differently depending on who sends the object.
 * {{{
 *     val currentTag: Current.type = Current //Bad & useless
 *     case class WeirdClass(tag: Current.type) //Bad & useless
 * }}}
 * */
class unstabletype extends StaticAnnotation {


}
