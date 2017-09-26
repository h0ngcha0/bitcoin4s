package me.hongchao.bitcoin4s.script

import cats.data.State

import scala.annotation.tailrec
import me.hongchao.bitcoin4s.Utils._
import me.hongchao.bitcoin4s.script.Interpreter._
import me.hongchao.bitcoin4s.script.ScriptFlag.SCRIPT_VERIFY_MINIMALDATA
import me.hongchao.bitcoin4s.script.InterpreterError._

import scala.util.{Failure, Success, Try}

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

  implicit val interpreter = new Interpretable[FlowControlOp] {
    def interpret(opCode: FlowControlOp): InterpreterContext = {
      State.get[InterpreterState].flatMap { state =>
        val requireMinimalEncoding: Boolean = state.flags.contains(SCRIPT_VERIFY_MINIMALDATA)

        opCode match {
          case OP_NOP =>
            State.set(state.copy(opCount = state.opCount + 1)).flatMap(continue)

          case OP_IF | OP_NOTIF =>
            Try {
              splitScriptOnConditional(
                script = state.script,
                nestedDepth = 1,
                onTrueBranch = true,
                acc = ConditionalBranchSplitResult()
              )
            } match {
              case Success(ConditionalBranchSplitResult(trueBranch, falseBranch, rest)) =>
                state.stack match {
                  case first :: tail =>
                    val firstNumber = ScriptNum(first.bytes, requireMinimalEncoding)
                    val newScript = (firstNumber == 0).option(falseBranch ++ rest).getOrElse(trueBranch ++ rest)
                    val newState = state.copy(
                      script = newScript,
                      stack = tail,
                      opCount = state.opCount + 1
                    )
                    State.set(newState).flatMap(continue)
                  case _ =>
                    throw NotEnoughElementsInStack(opCode, state.stack)
                }

              case Failure(error: InterpreterError) =>
                abort(error)

              case Failure(error) =>
                throw error
            }

          case OP_ELSE | OP_ENDIF =>
            abort(UnexpectedOpCode(opCode, state.stack))

          case OP_VERIFY =>
            state.stack match {
              case first :: tail =>
                val firstNumber = ScriptNum(first.bytes, requireMinimalEncoding)
                if (firstNumber == 0) {
                  abort(VerificationFailed(OP_VERIFY, state.stack))
                } else {
                  val newState = state.copy(
                    stack = tail,
                    opCount = state.opCount + 1
                  )
                  State.set(newState).flatMap(continue)
                }
              case _ =>
                abort(NotEnoughElementsInStack(OP_VERIFY, state.stack))
            }

          case OP_RETURN =>
            abort(UnexpectedOpCode(opCode, state.stack))
        }
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
        case opCode :: tail if opCode == OP_IF || opCode == OP_NOTIF =>
          val newNestedDepth = nestedDepth + 1

          if (newNestedDepth > 1){
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
          if (nestedDepth == 1) {
            acc.copy(rest = tail)
          } else if (nestedDepth > 1) {
            splitScriptOnConditional(
              script = tail,
              nestedDepth = nestedDepth - 1,
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

        case element :: tail if nestedDepth >= 1 =>
          splitScriptOnConditional(
            script = tail,
            nestedDepth = nestedDepth,
            onTrueBranch = onTrueBranch,
            acc = pushToBranch(element, onTrueBranch, acc)
          )
      }
    }

    private def pushToBranch(
      element: ScriptElement,
      onTrueBranch: Boolean,
      currentResult: ConditionalBranchSplitResult
    ): ConditionalBranchSplitResult = {
      onTrueBranch.option {
        currentResult.copy(trueBranch = currentResult.trueBranch :+ element)
      } getOrElse {
        currentResult.copy(falseBranch = currentResult.falseBranch :+ element)
      }
    }
  }
}
