package me.hongchao.bitcoin4s.script

import cats.data.State

import scala.annotation.tailrec
import me.hongchao.bitcoin4s.Utils._
import me.hongchao.bitcoin4s.script.Interpreter._
import me.hongchao.bitcoin4s.script.ScriptFlag.SCRIPT_VERIFY_MINIMALDATA
import me.hongchao.bitcoin4s.script.InterpreterError._

sealed trait FlowControlOp extends ScriptOpCode

object FlowControlOp {
  case object OP_NOP extends FlowControlOp { val value = 97 }
  case object OP_IF extends FlowControlOp { val value = 99 }
  case object OP_NOTIF extends FlowControlOp { val value = 100 }
  case object OP_ELSE extends FlowControlOp { val value = 103 }
  case object OP_ENDIF extends FlowControlOp { val value = 104 }
  case object OP_VERIFY extends FlowControlOp { val value = 105 }
  case object OP_RETURN extends FlowControlOp { val value = 106 }

  val all = Seq(OP_NOP, OP_IF, OP_NOTIF, OP_ELSE, OP_ENDIF, OP_VERIFY, OP_RETURN)

  implicit val interpreter2 = new Interpretable[FlowControlOp] {
    def interpret(opCode: FlowControlOp): InterpreterContext = {
      opCode match {
        case OP_NOP =>
          State.get[InterpreterState].flatMap { state =>
            State.set(state.copy(opCount = state.opCount + 1))
          }.flatMap(continue)
      }
    }

    case class ConditionalBranchSplitResult(
      trueBranch: Seq[ScriptElement] = Seq.empty,
      falseBranch: Seq[ScriptElement] = Seq.empty,
      rest: Seq[ScriptElement] = Seq.empty
    )

    @tailrec
    private def splitScriptOnConditional(
      script: Seq[ScriptElement],
      nestedDepth: Int = 0,
      onTrueBranch: Boolean = true,
      acc: ConditionalBranchSplitResult
    ): ConditionalBranchSplitResult = {
      script match {
        case Nil =>
          acc
        case opCode :: tail if opCode == OP_IF || opCode == OP_NOTIF =>
          val newNestedDepth = nestedDepth + 1

          if (newNestedDepth == 1) {
            splitScriptOnConditional(
              script = tail,
              nestedDepth = newNestedDepth,
              onTrueBranch = onTrueBranch,
              acc = acc
            )
          } else if (newNestedDepth > 1){
            splitScriptOnConditional(
              script = tail,
              nestedDepth = newNestedDepth,
              onTrueBranch = onTrueBranch,
              acc = pushToBranch(opCode, onTrueBranch, acc)
            )
          } else {
            throw new RuntimeException("Unbalanced conditionals")
          }

        case OP_ENDIF :: tail =>
          val newNestedDepth = nestedDepth - 1

          if (newNestedDepth == 0) {
            splitScriptOnConditional(
              script = tail,
              nestedDepth = newNestedDepth,
              onTrueBranch = onTrueBranch,
              acc = acc
            )
          } else if (newNestedDepth > 0) {
            splitScriptOnConditional(
              script = tail,
              nestedDepth = nestedDepth,
              onTrueBranch = onTrueBranch,
              acc = pushToBranch(OP_ENDIF, onTrueBranch, acc)
            )
          } else {
            throw new RuntimeException("Unbalanced conditionals")
          }

        case OP_ELSE :: tail =>
          if (nestedDepth == 1) {
            splitScriptOnConditional(
              script = tail,
              nestedDepth = nestedDepth,
              onTrueBranch = !onTrueBranch,  // invert the onTrueBranch value
              acc = acc
            )
          } else if (nestedDepth > 1){
            splitScriptOnConditional(
              script = tail,
              nestedDepth = nestedDepth,
              onTrueBranch = onTrueBranch,
              acc = pushToBranch(OP_ELSE, onTrueBranch, acc)
            )
          } else {
            throw new RuntimeException("Unbalanced conditionals")
          }
      }
    }

    private def pushToBranch(
      element: ScriptElement,
      onTrueBranch: Boolean,
      currentResult: ConditionalBranchSplitResult
    ): ConditionalBranchSplitResult = {
      onTrueBranch.option {
        currentResult.copy(trueBranch = element +: currentResult.trueBranch)
      } getOrElse {
        currentResult.copy(falseBranch = element +: currentResult.falseBranch)
      }
    }

  }

  implicit val interpreter = new Interpreter[FlowControlOp] {
    def interpret(opCode: FlowControlOp, context: InterpreterState): InterpreterState = {
      val requireMinimalEncoding: Boolean = context.flags.contains(SCRIPT_VERIFY_MINIMALDATA)

      opCode match {
        case OP_NOP =>
          context.copy(opCount = context.opCount + 1)

        case OP_IF | OP_NOTIF =>
          val ConditionalBranchSplitResult(trueBranch, falseBranch, rest) = splitScriptOnConditional(
            script = context.script,
            nestedDepth = 0,
            onTrueBranch = true,
            acc = ConditionalBranchSplitResult()
          )

          context.stack match {
            case first :: tail =>
              val firstNumber = ScriptNum(first.bytes, requireMinimalEncoding)
              val newScript = (firstNumber == 0).option(falseBranch ++ rest).getOrElse(trueBranch ++ rest)
              context.copy(
                script = newScript,
                stack = tail,
                opCount = context.opCount + 1
              )
            case _ =>
              throw NotEnoughElementsInStack(opCode, context.stack)
          }

        case OP_ELSE | OP_ENDIF =>
          throw new RuntimeException(s"$opCode should not be consumed when interpreting OP_IF")

        case OP_VERIFY =>
          context.stack match {
            case first :: tail =>
              val firstNumber = ScriptNum(first.bytes, requireMinimalEncoding)
              if (firstNumber == 0) {
                throw VerificationFailed(OP_VERIFY, context.stack)
              } else {
                context.copy(
                  script = context.script.tail,
                  stack = tail,
                  opCount = context.opCount + 1
                )
              }
            case _ =>
              throw NotEnoughElementsInStack(OP_VERIFY, context.stack)
          }


        case OP_RETURN =>
          // FIXME: better error handling
          throw new RuntimeException("OP_RETURN is evaluated")
      }
    }

    case class ConditionalBranchSplitResult(
      trueBranch: Seq[ScriptElement] = Seq.empty,
      falseBranch: Seq[ScriptElement] = Seq.empty,
      rest: Seq[ScriptElement] = Seq.empty
    )

    @tailrec
    private def splitScriptOnConditional(
      script: Seq[ScriptElement],
      nestedDepth: Int = 0,
      onTrueBranch: Boolean = true,
      acc: ConditionalBranchSplitResult
    ): ConditionalBranchSplitResult = {
      script match {
        case Nil =>
          acc
        case opCode :: tail if opCode == OP_IF || opCode == OP_NOTIF =>
          val newNestedDepth = nestedDepth + 1

          if (newNestedDepth == 1) {
            splitScriptOnConditional(
              script = tail,
              nestedDepth = newNestedDepth,
              onTrueBranch = onTrueBranch,
              acc = acc
            )
          } else if (newNestedDepth > 1){
            splitScriptOnConditional(
              script = tail,
              nestedDepth = newNestedDepth,
              onTrueBranch = onTrueBranch,
              acc = pushToBranch(opCode, onTrueBranch, acc)
            )
          } else {
            throw new RuntimeException("Unbalanced conditionals")
          }

        case OP_ENDIF :: tail =>
          val newNestedDepth = nestedDepth - 1

          if (newNestedDepth == 0) {
            splitScriptOnConditional(
              script = tail,
              nestedDepth = newNestedDepth,
              onTrueBranch = onTrueBranch,
              acc = acc
            )
          } else if (newNestedDepth > 0) {
            splitScriptOnConditional(
              script = tail,
              nestedDepth = nestedDepth,
              onTrueBranch = onTrueBranch,
              acc = pushToBranch(OP_ENDIF, onTrueBranch, acc)
            )
          } else {
            throw new RuntimeException("Unbalanced conditionals")
          }

        case OP_ELSE :: tail =>
          if (nestedDepth == 1) {
            splitScriptOnConditional(
              script = tail,
              nestedDepth = nestedDepth,
              onTrueBranch = !onTrueBranch,  // invert the onTrueBranch value
              acc = acc
            )
          } else if (nestedDepth > 1){
            splitScriptOnConditional(
              script = tail,
              nestedDepth = nestedDepth,
              onTrueBranch = onTrueBranch,
              acc = pushToBranch(OP_ELSE, onTrueBranch, acc)
            )
          } else {
            throw new RuntimeException("Unbalanced conditionals")
          }
      }
    }

    private def pushToBranch(
      element: ScriptElement,
      onTrueBranch: Boolean,
      currentResult: ConditionalBranchSplitResult
    ): ConditionalBranchSplitResult = {
      onTrueBranch.option {
        currentResult.copy(trueBranch = element +: currentResult.trueBranch)
      } getOrElse {
        currentResult.copy(falseBranch = element +: currentResult.falseBranch)
      }
    }
  }
}
