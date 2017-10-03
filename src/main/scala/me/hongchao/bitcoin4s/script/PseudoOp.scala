package me.hongchao.bitcoin4s.script

import cats.data.{State, StateT}
import me.hongchao.bitcoin4s.script.Interpreter._
import me.hongchao.bitcoin4s.script.InterpreterError.NotImplemented
import cats.implicits._

sealed trait PseudoOp extends ScriptOpCode

object PseudoOp {
  case object OP_PUBKEYHASH extends PseudoOp { val value = 253 }
  case object OP_PUBKEY extends PseudoOp { val value = 254 }
  case object OP_INVALIDOPCODE extends PseudoOp { val value = 255 }

  val all = Seq(OP_PUBKEYHASH, OP_PUBKEY, OP_INVALIDOPCODE)

  implicit val interpreter = new Interpretable[PseudoOp] {
    def interpret(opCode: PseudoOp): InterpreterContext[Option[Boolean]] = {
      getState.flatMap { state =>
        abort(NotImplemented(opCode, state.stack))
      }
    }
  }
}