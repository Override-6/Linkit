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

object BPController {

    def getOperator(op: String): Operator = {
        op match {
            case "==" => Equals(false)
            case "!=" => Equals(true)
            case ">"  => Upper
            case ">"  => Lower
        }
    }

    def getValue(clause: String): Value = {
        val firstChar = clause.head
        val lastChar  = clause.last
        if (firstChar == '\"' || lastChar == '\"') {
            if (firstChar != lastChar)
                throw new IllegalArgumentException(s"Wrong string expression '$clause'")
            StaticValue(clause)
        }
        else {
            if (clause.count(_.isWhitespace) > 0)
                throw new IllegalArgumentException(s"Expected Blueprint value. got '$clause'")
            BPValue(clause)
        }
    }

    case class IfElif(override val blockPosition: Int,
                      override val expressionPosition: Int,
                      override val layerBlueprint: String,
                      expression: Array[String],
                      @Nullable override val before: BlueprintFlowController) extends BlueprintFlowController {

        protected val left    : BPController.Value    = getValue(expression(0))
        protected val operator: BPController.Operator = getOperator(expression(1))
        protected val right   : BPController.Value    = getValue(expression(2))

        override def getBlueprint(context: GenerationContext): (String, Int) = {
            def get: (String, Int) = {
                if (operator.test(left, right)(context))
                    (layerBlueprint, blockPosition)
                else null
            }

            if (before == null)
                get
            else {
                val result = before.getBlueprint(context)
                if (result == null) {
                    get
                } else
                    result
            }
        }
    }

    case class Else(override val blockPosition: Int,
                    override val expressionPosition: Int,
                    override val layerBlueprint: String,
                    @NotNull override val before: BlueprintFlowController) extends BlueprintFlowController {

        override def getBlueprint(context: GenerationContext): (String, Int) = {
            val result = before.getBlueprint(context)
            if (result == null) (layerBlueprint, blockPosition)
            else result
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
            (left.get(context) == right.get(context)) != invert
        }
    }

    private object Upper extends Operator {

        override def test(left: Value, right: Value)(implicit context: GenerationContext): Boolean = left.get.toInt > right.get.toInt
    }

    private object Lower extends Operator {

        override def test(left: Value, right: Value)(implicit context: GenerationContext): Boolean = left.get.toInt < right.get.toInt
    }

}
