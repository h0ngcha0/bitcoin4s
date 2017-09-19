package me.hongchao.bitcoin4s.script

import cats.Eval
import cats.data.{State, StateT}
import me.hongchao.bitcoin4s.script.Interpreter._
import me.hongchao.bitcoin4s.script.InterpreterError._

sealed trait StackOp extends ScriptOpCode

object StackOp {
  case object OP_TOALTSTACK extends StackOp { val value = 107 }
  case object OP_FROMALTSTACK extends StackOp { val value = 108 }
  case object OP_2DROP extends StackOp { val value = 109 }
  case object OP_2DUP extends StackOp { val value = 110 }
  case object OP_3DUP extends StackOp { val value = 111 }
  case object OP_2OVER extends StackOp { val value = 112 }
  case object OP_2ROT extends StackOp { val value = 113 }
  case object OP_2SWAP extends StackOp { val value = 114 }
  case object OP_IFDUP extends StackOp { val value = 115 }
  case object OP_DEPTH extends StackOp { val value = 116 }
  case object OP_DROP extends StackOp { val value = 117 }
  case object OP_DUP extends StackOp { val value = 118 }
  case object OP_NIP extends StackOp { val value = 119 }
  case object OP_OVER extends StackOp { val value = 120 }
  case object OP_PICK extends StackOp { val value = 121 }
  case object OP_ROLL extends StackOp { val value = 122 }
  case object OP_ROT extends StackOp { val value = 123 }
  case object OP_SWAP extends StackOp { val value = 124 }
  case object OP_TUCK extends StackOp { val value = 125 }

  val all = Seq(
    OP_TOALTSTACK, OP_FROMALTSTACK, OP_2DROP, OP_2DUP, OP_3DUP, OP_2OVER,
    OP_2ROT, OP_2SWAP, OP_IFDUP, OP_DEPTH, OP_DROP, OP_DUP, OP_NIP, OP_OVER,
    OP_PICK, OP_ROLL, OP_ROT, OP_SWAP, OP_TUCK
  )

  implicit val interpreter = new Interpretable[StackOp] {
    def interpret(opCode: StackOp): InterpreterContext = {
      opCode match {
        case OP_DUP =>
          onStackOp(OP_DUP) {
            case head :: rest =>
              head :: head :: rest
          }

        case OP_TOALTSTACK =>
          State.get[InterpreterState].flatMap { state =>
            val stack = state.stack

            stack match {
              case head :: _ =>
                val newState = state.copy(
                  stack = state.stack.tail,
                  altStack = head +: state.altStack,
                  opCount = state.opCount + 1
                )
                State.set(newState).flatMap(continue)
              case _ =>
                abort(NotEnoughElementsInStack(OP_TOALTSTACK, stack))

            }
          }

        case OP_FROMALTSTACK =>
          State.get[InterpreterState].flatMap { state =>
            state.altStack match {
              case head :: _ =>
                val newState = state.copy(
                  stack = head +: state.stack,
                  altStack = state.altStack.tail
                )

                State.set(newState).flatMap(continue)
              case _ =>
                abort(NotEnoughElementsInAltStack(OP_FROMALTSTACK, state.stack))
            }
          }

        case OP_DROP =>
          onStackOp(OP_2DROP) {
            case _ :: rest =>
              rest
          }

        case OP_2DROP =>
          onStackOp(OP_2DROP) {
            case _ :: _ :: rest =>
              rest
          }

        case OP_2DUP =>
          onStackOp(OP_2DUP) {
            case first :: second :: rest =>
              first :: second :: first :: second :: rest
          }

        case OP_3DUP =>
          onStackOp(OP_3DUP) {
            case first :: second :: third :: rest =>
              first :: second :: third :: first :: second :: third :: rest
          }

        case OP_OVER =>
          onStackOp(OP_OVER) {
            case first :: second :: rest =>
              second :: first :: second :: rest
          }

        case OP_2OVER =>
          onStackOp(OP_2OVER) {
            case first :: second :: third :: fourth :: rest =>
              third :: fourth :: first :: second :: third :: fourth :: rest
          }

        case OP_ROT =>
          onStackOp(OP_ROT) {
            case first :: second :: third :: rest =>
              third :: first :: second :: third :: rest
          }

        case OP_2ROT =>
          onStackOp(OP_2ROT) {
            case first :: second :: third :: fourth :: fifth :: sixth :: rest =>
              fifth :: sixth :: first :: second :: third :: fourth :: rest
          }

        case OP_SWAP =>
          onStackOp(OP_SWAP) {
            case first :: second :: rest =>
              second :: first :: rest
          }

        case OP_2SWAP =>
          onStackOp(OP_2SWAP) {
            case first :: second :: third :: fourth :: rest =>
              third :: fourth :: first :: second :: rest
          }

        case OP_IFDUP =>
          onStackOp(OP_IFDUP) {
            case (number: ScriptNum) :: rest if (number.value == 0)=>
              ScriptNum(0) :: ScriptNum(0) :: rest
            case first :: rest =>
              first :: rest
          }

        case OP_DEPTH =>
          State.get[InterpreterState].flatMap { state =>
            val newStack = ScriptNum(state.stack.length) +: state.stack
            State.set(state.copy(stack = newStack, opCount = state.opCount + 1)).flatMap(continue)
          }

        case OP_NIP =>
          onStackOp(OP_NIP) {
            case first :: second :: rest =>
              first :: rest
          }

        case OP_PICK =>
          State.get[InterpreterState].flatMap { state =>
            val stack = state.stack

            stack match {
              case (number: ScriptNum) :: rest if rest.length >= number.value =>
                val newState = rest(number.value.toInt) :: rest
                State.set(state.copy(stack = newState, opCount = state.opCount + 1)).flatMap(continue)
              case (number: ScriptNum) :: rest if rest.length < number.value =>
                abort(NotEnoughElementsInStack(OP_PICK, stack))
              case _ :: rest =>
                abort(OperantMustBeScriptNum(OP_PICK, stack))
              case _ =>
                abort(NotEnoughElementsInStack(OP_PICK, stack))
            }
          }

        case OP_ROLL =>
          State.get[InterpreterState].flatMap { state =>
            val stack = state.stack

            stack match {
              case (number: ScriptNum) :: rest if rest.length >= number.value =>
                val newState = rest(number.value.toInt) :: (rest.take(number.value.toInt) ++ rest.drop(number.value.toInt+1))
                State.set(state.copy(stack = newState, opCount = state.opCount + 1)).flatMap(continue)
              case (number: ScriptNum) :: rest if rest.length < number.value =>
                abort(NotEnoughElementsInStack(OP_ROLL, stack))
              case _ :: rest =>
                abort(OperantMustBeScriptNum(OP_ROLL, stack))
              case _ =>
                abort(NotEnoughElementsInStack(OP_ROLL, stack))
            }
          }

        case OP_TUCK =>
          onStackOp(OP_TUCK) {
            case first :: second :: rest =>
              first :: second :: first :: rest
          }
      }
    }

    def onStackOp(opCode: ScriptOpCode)(stackConvertFunction: PartialFunction[Seq[ScriptElement], Seq[ScriptElement]]): InterpreterContext = {
      State.get[InterpreterState].flatMap { state =>
        if (stackConvertFunction.isDefinedAt(state.stack)) {
          val newStack = stackConvertFunction(state.stack)
          State.set(state.copy(stack = newStack, opCount = state.opCount + 1)).flatMap(continue)
        } else {
          abort(NotEnoughElementsInStack(opCode, state.stack))
        }
      }
    }
  }
}
