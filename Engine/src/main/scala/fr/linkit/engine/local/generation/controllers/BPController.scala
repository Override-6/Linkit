/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.local.generation.controllers

import fr.linkit.api.local.generation.{BlueprintFlowController, GenerationContext}
import org.jetbrains.annotations.{NotNull, Nullable}

import java.util.regex.{MatchResult, Pattern}

object BPController {

    val OpPattern: Pattern = Pattern.compile("\"[^\"]+\"|(!=|==|>|<|<=|>=)")

    def getOperator(op: String): Operator = {
        op match {
            case "==" => Equals(invert = false)
            case "!=" => Equals(invert = true)
            case ">"  => Upper(orEquals = false)
            case ">=" => Upper(orEquals = true)
            case "<"  => Lower(orEquals = false)
            case "<=" => Lower(orEquals = true)
        }
    }

    def getValue(clause: String): Value = {
        val trimClause = clause.trim
        val firstChar = trimClause.head
        val lastChar  = trimClause.last
        if (firstChar == '\"' || lastChar == '\"') {
            if (firstChar != lastChar)
                throw new IllegalArgumentException(s"Wrong string expression '$trimClause'")
            StaticValue(trimClause.slice(1, trimClause.length - 1))
        }
        else {
            if (trimClause.count(_.isWhitespace) > 0)
                throw new IllegalArgumentException(s"Expected Blueprint value. got '$trimClause'")
            BPValue(trimClause)
        }
    }

    case class IfElif(override val blockPosition: Int,
                      override val expressionPosition: Int,
                      override val layerBlueprint: String,
                      expression: String,
                      @Nullable override val before: BlueprintFlowController) extends BlueprintFlowController {

        protected val (left, operator, right) = compile()

        override def getBlueprint(context: GenerationContext): Option[(String, Int)] = {
            def get: Option[(String, Int)] = {
                if (operator.test(left, right)(context))
                    Some((layerBlueprint, blockPosition))
                else None
            }

            if (before == null)
                get
            else
                before.getBlueprint(context).orElse(get)
        }

        private def compile(): (Value, Operator, Value) = {
            val results = OpPattern.matcher(expression)
                    .results()
                    .toArray(new Array[MatchResult](_))
            val count   = results.count(_.group(1) != null)
            if (count != 1)
                throw new IllegalArgumentException(s"Invalid expression (no operator found or more than one operator has been written): $expression")

            val result   = results.find(_.group(1) != null).get
            val operator = getOperator(result.group(1))
            val left     = getValue(expression.take(result.start()))
            val right    = getValue(expression.drop(result.end()))
            (left, operator, right)
        }
    }

    case class Else(override val blockPosition: Int,
                    override val expressionPosition: Int,
                    override val layerBlueprint: String,
                    @NotNull override val before: BlueprintFlowController) extends BlueprintFlowController {

        override def getBlueprint(context: GenerationContext): Option[(String, Int)] = {
            before
                    .getBlueprint(context)
                    .orElse(Some((layerBlueprint, blockPosition)))
        }
    }

    sealed trait Value {

        def get(implicit context: GenerationContext): String
    }

    private case class BPValue(value: String) extends Value {

        override def get(implicit context: GenerationContext): String = context.getValue(value)
    }

    private case class StaticValue(value: String) extends Value {

        override def get(implicit context: GenerationContext): String = value
    }

    sealed trait Operator {

        def test(left: Value, right: Value)(implicit context: GenerationContext): Boolean
    }

    private case class Equals(invert: Boolean) extends Operator {

        override def test(left: Value, right: Value)(implicit context: GenerationContext): Boolean = {
            (left.get == right.get) != invert
        }
    }

    private case class Upper(orEquals: Boolean) extends Operator {

        override def test(left: Value, right: Value)(implicit context: GenerationContext): Boolean = {
            if (orEquals)
                left.get.toInt >= right.get.toInt
            else
                left.get.toInt > right.get.toInt
        }
    }

    private case class Lower(orEquals: Boolean) extends Operator {

        override def test(left: Value, right: Value)(implicit context: GenerationContext): Boolean = {
            if (orEquals)
                left.get.toInt <= right.get.toInt
            else
                left.get.toInt < right.get.toInt
        }
    }

}
