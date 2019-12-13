package it.softfork.bitcoin4s.script

import it.softfork.bitcoin4s.script.Interpreter._
import it.softfork.bitcoin4s.script.InterpreterError._
import cats.implicits._

sealed trait SpliceOp extends ScriptOpCode

object SpliceOp {
  case object OP_CAT extends SpliceOp { val value = 126 }
  case object OP_SUBSTR extends SpliceOp { val value = 127 }
  case object OP_LEFT extends SpliceOp { val value = 128 }
  case object OP_RIGHT extends SpliceOp { val value = 129 }
  case object OP_SIZE extends SpliceOp { val value = 130 }

  val all = Set(
    OP_CAT,
    OP_SUBSTR,
    OP_LEFT,
    OP_RIGHT,
    OP_SIZE
  )

  val disabled = Seq(OP_CAT, OP_SUBSTR, OP_LEFT, OP_RIGHT)

  implicit val interpreter = new InterpretableOp[SpliceOp] {

    def interpret(opCode: SpliceOp): InterpreterContext[Option[Boolean]] = {
      getState.flatMap { state =>
        opCode match {
          case opc if disabled.contains(opc) =>
            abort(OpcodeDisabled(opc, state))

          case OP_SIZE =>
            state.stack match {
              case head :: _ =>
                val newState = state.copy(
                  currentScript = state.currentScript,
                  stack = ScriptNum(head.bytes.length) +: state.stack,
                  opCount = state.opCount + 1
                )

                setStateAndContinue(newState)
              case Nil =>
                abort(InvalidStackOperation(OP_SIZE, state))
            }
        }
      }
    }
  }
}
