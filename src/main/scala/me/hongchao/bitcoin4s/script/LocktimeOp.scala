package me.hongchao.bitcoin4s.script

import me.hongchao.bitcoin4s.script.Interpreter._
import me.hongchao.bitcoin4s.script.InterpreterError._
import cats.implicits._

sealed trait LocktimeOp extends ScriptOpCode

object LocktimeOp {
  case object OP_CHECKLOCKTIMEVERIFY extends LocktimeOp { val value = 177 }
  case object OP_CHECKSEQUENCEVERIFY extends LocktimeOp { val value = 178 }
  case object OP_NOP2 extends LocktimeOp { val value = 177 }
  case object OP_NOP3 extends LocktimeOp { val value = 178 }

  val all = Seq(OP_CHECKLOCKTIMEVERIFY, OP_CHECKSEQUENCEVERIFY, OP_NOP2, OP_NOP3)

  implicit val interpreter = new Interpretable[LocktimeOp] {
    def interpret(opCode: LocktimeOp): InterpreterContext[Option[Boolean]] = {
      opCode match {
        case OP_CHECKLOCKTIMEVERIFY | OP_NOP2 =>
          getState.flatMap { state =>
            if (state.cltvEnabled) {
              state.stack match {
                case head :: _ =>
                  val lockTime = ScriptNum(head.bytes, false, 5).value

                  if (lockTime > state.transaction.lock_time) {
                    setState(state.replaceStackTopElement(ScriptNum(1))).flatMap(continue)
                  } else {
                    abort(CLTVFailed(opCode, state.stack))
                  }

                case Nil =>
                  abort(NotEnoughElementsInStack(opCode, state.stack))
              }
            } else if (state.disCourageUpgradableNop) {
              abort(DiscourageUpgradableNops(opCode, state.stack))
            } else {
              continue(opCode)
            }
          }

        case OP_CHECKSEQUENCEVERIFY | OP_NOP3 =>
          getState.flatMap { state =>
            if (state.csvEnabled) {
              state.stack match {
                case head :: _ =>
                  val sequence = ScriptNum(head.bytes, false, 5).value
                  val input = state.transaction.tx_in(state.inputIndex)
                  if (sequence >= input.sequence) {
                    setState(state.replaceStackTopElement(ScriptNum(1))).flatMap(continue)
                  } else {
                    abort(CLTVFailed(opCode, state.stack))
                  }

                case Nil =>
                  abort(NotEnoughElementsInStack(opCode, state.stack))
              }
            } else if (state.disCourageUpgradableNop) {
              abort(DiscourageUpgradableNops(opCode, state.stack))
            } else {
              continue(opCode)
            }
          }
      }
    }
  }
}