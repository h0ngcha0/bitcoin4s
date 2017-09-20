package me.hongchao.bitcoin4s.script

import cats.data.State
import me.hongchao.bitcoin4s.Utils._
import me.hongchao.bitcoin4s.script.FlowControlOp.OP_VERIFY
import me.hongchao.bitcoin4s.script.Interpreter._
import me.hongchao.bitcoin4s.script.InterpreterError._

sealed trait BitwiseLogicOp extends ScriptOpCode

object BitwiseLogicOp {
  case object OP_INVERT extends BitwiseLogicOp { val value = 131 }
  case object OP_AND extends BitwiseLogicOp { val value = 132 }
  case object OP_OR extends BitwiseLogicOp { val value = 133 }
  case object OP_XOR extends BitwiseLogicOp { val value = 134 }
  case object OP_EQUAL extends BitwiseLogicOp { val value = 135 }
  case object OP_EQUALVERIFY extends BitwiseLogicOp { val value = 136 }

  val all = Seq(OP_INVERT, OP_AND, OP_OR, OP_XOR, OP_EQUAL, OP_EQUALVERIFY)
  val disabled = Seq(OP_INVERT, OP_AND, OP_OR, OP_XOR)

  implicit val interpreter = new Interpretable[BitwiseLogicOp] {
    override def interpret(opCode: BitwiseLogicOp): InterpreterContext = {
      opCode match {
        case opc if disabled.contains(opc) =>
          State.get.flatMap( state =>
            abort(OpcodeDisabled(opc, state.stack))
          )

        case OP_EQUAL =>
          onOpEqual()

        case OP_EQUALVERIFY =>
          for {
            result <- onOpEqual()
            state <- State.get
            _ <- State.set(state.copy(script = OP_VERIFY +: state.script))
          } yield result // FIXME: should abort early if onOpEqual fails
      }
    }

    private def onOpEqual(): InterpreterContext = {
      State.get.flatMap { state =>
        state.stack match {
          case first :: second :: rest =>
            val result = (first.bytes == second.bytes).option(ScriptNum(1)).getOrElse(ScriptNum(0))

            val newState = state.copy(
              script = state.script.tail,
              stack = result +: rest,
              opCount = state.opCount + 1
            )

            State.set(newState).flatMap(continue)
          case _ =>
            abort(NotEnoughElementsInStack(OP_EQUAL, state.stack))
        }
      }
    }
  }
}