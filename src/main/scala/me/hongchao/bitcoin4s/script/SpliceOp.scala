package me.hongchao.bitcoin4s.script

import cats.data.State
import me.hongchao.bitcoin4s.script.Interpreter._
import me.hongchao.bitcoin4s.script.InterpreterError._

sealed trait SpliceOp extends ScriptOpCode

object SpliceOp {
  case object OP_CAT extends SpliceOp { val value = 126 }
  case object OP_SUBSTR extends SpliceOp { val value = 127 }
  case object OP_LEFT extends SpliceOp { val value = 128 }
  case object OP_RIGHT extends SpliceOp { val value = 129 }
  case object OP_SIZE extends SpliceOp { val value = 130 }

  val all = Seq(OP_CAT, OP_SUBSTR, OP_LEFT, OP_RIGHT, OP_SIZE)
  val disabled = Seq(OP_CAT, OP_SUBSTR, OP_LEFT, OP_RIGHT)

  implicit val interpreter = new Interpretable[SpliceOp] {
    def interpret(opCode: SpliceOp): InterpreterContext = {
      State.get[InterpreterState].flatMap { state =>
        opCode match {
          case opc if disabled.contains(opc) =>
            abort(OpcodeDisabled(opc, state.stack))

          case OP_SIZE =>
            state.stack match {
              case head :: _ =>
                val newState = state.copy(
                  script = state.script,
                  stack = ScriptNum(head.bytes.length) +: state.stack,
                  opCount = state.opCount + 1
                )

                State.set(newState).flatMap(continue)
              case Nil =>
                abort(NotEnoughElementsInStack(OP_SIZE, state.stack))
            }
        }
      }
    }
  }
}

