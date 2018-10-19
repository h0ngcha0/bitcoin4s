package it.softfork.bitcoin4s.script

import it.softfork.bitcoin4s.script.Interpreter._
import it.softfork.bitcoin4s.script.InterpreterError._
import cats.implicits._
import tech.minna.utilities.TraitEnumeration

import scala.util.{Failure, Success, Try}

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

  val all = TraitEnumeration.values[StackOp]

  implicit val interpreter = new InterpretableOp[StackOp] {

    def interpret(opCode: StackOp): InterpreterContext[Option[Boolean]] = {
      opCode match {
        case OP_DUP =>
          onStackOp(OP_DUP) {
            case head :: rest =>
              head :: head :: rest
          }

        case OP_TOALTSTACK =>
          getState.flatMap { state =>
            val stack = state.stack

            stack match {
              case head :: _ =>
                val newState = state.copy(
                  stack = state.stack.tail,
                  altStack = head +: state.altStack,
                  opCount = state.opCount + 1
                )
                setStateAndContinue(newState)
              case _ =>
                abort(InvalidStackOperation(OP_TOALTSTACK, state))
            }
          }

        case OP_FROMALTSTACK =>
          getState.flatMap { state =>
            state.altStack match {
              case head :: _ =>
                val newState = state.copy(
                  stack = head +: state.stack,
                  altStack = state.altStack.tail
                )

                setStateAndContinue(newState)
              case _ =>
                abort(InvalidAltStackOperation(OP_FROMALTSTACK, state))
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
              third :: first :: second :: rest
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
            case (number: ScriptConstant) :: rest if (ScriptNum.toLong(number.bytes) != 0) =>
              number :: number :: rest
            case first :: rest =>
              first :: rest
          }

        case OP_DEPTH =>
          getState.flatMap { state =>
            val newStack = ScriptNum(state.stack.length) +: state.stack
            setStateAndContinue(state.copy(stack = newStack, opCount = state.opCount + 1))
          }

        case OP_NIP =>
          onStackOp(OP_NIP) {
            case first :: (second @ _) :: rest =>
              first :: rest
          }

        case OP_PICK =>
          getState.flatMap { state =>
            state.stack match {
              case (constant: ScriptConstant) :: rest =>
                Try {
                  ScriptNum(constant.bytes, state.ScriptFlags.requireMinimalEncoding).value.toInt
                } match {
                  case Success(nth) =>
                    if (rest.nonEmpty && nth >= 0 && (rest.length >= (nth + 1))) {
                      val newState = rest(nth) :: rest
                      setStateAndContinue(state.copy(stack = newState, opCount = state.opCount + 1))
                    } else {
                      abort(InvalidStackOperation(OP_PICK, state))
                    }
                  case Failure(_) =>
                    abort(GeneralError(OP_PICK, state))
                }

              case _ :: _ =>
                abort(OperantMustBeScriptNum(OP_PICK, state))

              case _ =>
                abort(InvalidStackOperation(OP_PICK, state))
            }
          }

        case OP_ROLL =>
          getState.flatMap { state =>
            val stack = state.stack

            stack match {
              case (constant: ScriptConstant) :: rest =>
                Try {
                  ScriptNum(constant.bytes, state.ScriptFlags.requireMinimalEncoding).value.toInt
                } match {
                  case Success(nth) =>
                    if (rest.nonEmpty && nth >= 0 && (rest.length >= (nth + 1))) {
                      val newState = rest(nth) :: (rest.take(nth) ++ rest.drop(nth + 1))
                      setStateAndContinue(state.copy(stack = newState, opCount = state.opCount + 1))
                    } else {
                      abort(InvalidStackOperation(OP_ROLL, state))
                    }

                  case Failure(_) =>
                    abort(GeneralError(OP_ROLL, state))
                }

              case _ :: _ =>
                abort(OperantMustBeScriptNum(OP_ROLL, state))

              case _ =>
                abort(InvalidStackOperation(OP_ROLL, state))
            }
          }

        case OP_TUCK =>
          onStackOp(OP_TUCK) {
            case first :: second :: rest =>
              first :: second :: first :: rest
          }
      }
    }

    def onStackOp(
      opCode: ScriptOpCode
    )(stackConvertFunction: PartialFunction[Seq[ScriptElement], Seq[ScriptElement]]): InterpreterContext[Option[Boolean]] = {
      getState.flatMap { state =>
        if (stackConvertFunction.isDefinedAt(state.stack)) {
          val newStack = stackConvertFunction(state.stack)
          setStateAndContinue(state.copy(stack = newStack, opCount = state.opCount + 1))
        } else {
          abort(InvalidStackOperation(opCode, state))
        }
      }
    }
  }
}
