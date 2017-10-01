package me.hongchao.bitcoin4s.script

import cats.data.State
import me.hongchao.bitcoin4s.script.Interpreter._
import me.hongchao.bitcoin4s.script.InterpreterError._

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
        val cltvEnabled = state.flags.contains(ScriptFlag.SCRIPT_VERIFY_CHECKLOCKTIMEVERIFY)
        val disCourageUpgradableNop = state.flags.contains(ScriptFlag.SCRIPT_VERIFY_DISCOURAGE_UPGRADABLE_NOPS)
        if (cltvEnabled) {
          state.stack match {
            case head :: _ =>
              val lockTime = ScriptNum(head.bytes, false, 5).value

              if (lockTime > state.transaction.lock_time) {
                State.set(state.dropTopElement).flatMap(continue)
              } else {
                abort(CLTVFailed(opCode, state.stack))
              }

            case Nil =>
              abort(NotEnoughElementsInStack(opCode, state.stack))
          }
        } else if (disCourageUpgradableNop) {
          abort(DiscourageUpgradableNops(opCode, state.stack))
        } else {
          State.pure(Right(None))
        }
      }
    }
  }
}