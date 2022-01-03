package fr.linkit.engine.internal.language.bhv.parse

import fr.linkit.engine.internal.language.bhv.parse.line.Expression

import java.util.Scanner
import scala.collection.mutable

class BhvFileReader(scanner: Scanner) {

    private val pendingExpressions = mutable.Queue.empty[Expression]

    private def getNextExpressionInQueue(expressionKind: String): Option[Expression] = {
        pendingExpressions.dequeueFirst(_.kind == expressionKind)
    }

    def getNextExpression(expressionKind: String): Expression =
        getNextExpressionInQueue(expressionKind).getOrElse {
        var nextExpression = getNextExpression
        while (nextExpression.kind != expressionKind) {
            pendingExpressions += nextExpression
            nextExpression = getNextExpression
        }
        nextExpression
    }

    def getNextExpression: Expression = {

    }

}
