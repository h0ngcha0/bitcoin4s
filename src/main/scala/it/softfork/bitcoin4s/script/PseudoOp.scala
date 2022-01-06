package it.softfork.bitcoin4s.script

import cats.implicits._

import it.softfork.bitcoin4s.script.Interpreter._
import it.softfork.bitcoin4s.script.InterpreterError.BadOpCode

sealed trait PseudoOp extends ScriptOpCode

object PseudoOp {
  case object OP_PUBKEYHASH extends PseudoOp { val value = 253 }
  case object OP_PUBKEY extends PseudoOp { val value = 254 }
  case object OP_INVALIDOPCODE extends PseudoOp { val value = 255 }

  val all = Set(OP_PUBKEY, OP_PUBKEYHASH, OP_INVALIDOPCODE)

  implicit val interpreter = new InterpretableOp[PseudoOp] {

    def interpret(opCode: PseudoOp): InterpreterContext[Option[Boolean]] = {
      getState.flatMap { state =>
        abort(BadOpCode(opCode, state, "Invalid opCode when executed"))
      }
    }
  }
}
