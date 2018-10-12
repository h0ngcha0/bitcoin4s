package it.softfork.bitcoin4s.script

import it.softfork.bitcoin4s.script.Interpreter._
import it.softfork.bitcoin4s.script.InterpreterError.BadOpCode
import tech.minna.utilities.TraitEnumeration
import cats.implicits._

sealed trait PseudoOp extends ScriptOpCode

object PseudoOp {
  case object OP_PUBKEYHASH extends PseudoOp { val value = 253 }
  case object OP_PUBKEY extends PseudoOp { val value = 254 }
  case object OP_INVALIDOPCODE extends PseudoOp { val value = 255 }

  val all = TraitEnumeration.values[PseudoOp]

  implicit val interpreter = new InterpretableOp[PseudoOp] {
    def interpret(opCode: PseudoOp): InterpreterContext[Option[Boolean]] = {
      getState.flatMap { state =>
        abort(BadOpCode(opCode, state, "Invalid opCode when executed"))
      }
    }
  }
}