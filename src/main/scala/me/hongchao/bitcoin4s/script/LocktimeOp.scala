package me.hongchao.bitcoin4s.script

import cats.data.State
import me.hongchao.bitcoin4s.script.Interpreter._
import me.hongchao.bitcoin4s.script.InterpreterError.NotImplemented

sealed trait LocktimeOp extends ScriptOpCode

object LocktimeOp {
  case object OP_CHECKLOCKTIMEVERIFY extends LocktimeOp { val value = 177 }
  case object OP_CHECKSEQUENCEVERIFY extends LocktimeOp { val value = 178 }
  case object OP_NOP2 extends LocktimeOp { val value = 177 }
  case object OP_NOP3 extends LocktimeOp { val value = 178 }

  val all = Seq(OP_CHECKLOCKTIMEVERIFY, OP_CHECKSEQUENCEVERIFY, OP_NOP2, OP_NOP3)

  implicit val interpreter = new Interpretable[LocktimeOp] {
    def interpret(opCode: LocktimeOp): InterpreterContext = {
      State.get.flatMap { state =>
        abort(NotImplemented(opCode, state.stack))
      }
    }
  }
}