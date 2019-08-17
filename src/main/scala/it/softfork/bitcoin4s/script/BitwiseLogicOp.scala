package it.softfork.bitcoin4s.script

import it.softfork.bitcoin4s.Utils._
import it.softfork.bitcoin4s.script.FlowControlOp.OP_VERIFY
import it.softfork.bitcoin4s.script.Interpreter._
import it.softfork.bitcoin4s.script.InterpreterError._
import cats.implicits._

sealed trait BitwiseLogicOp extends ScriptOpCode

object BitwiseLogicOp {
  case object OP_INVERT extends BitwiseLogicOp { val value = 131 }
  case object OP_AND extends BitwiseLogicOp { val value = 132 }
  case object OP_OR extends BitwiseLogicOp { val value = 133 }
  case object OP_XOR extends BitwiseLogicOp { val value = 134 }
  case object OP_EQUAL extends BitwiseLogicOp { val value = 135 }
  case object OP_EQUALVERIFY extends BitwiseLogicOp { val value = 136 }

  val all = Set(
    OP_INVERT, OP_AND, OP_OR, OP_XOR, OP_EQUAL, OP_EQUALVERIFY
  )

  val disabled = Seq(OP_INVERT, OP_AND, OP_OR, OP_XOR)

  implicit val interpreter = new InterpretableOp[BitwiseLogicOp] {
    override def interpret(opCode: BitwiseLogicOp): InterpreterContext[Option[Boolean]] = {
      opCode match {
        case opc if disabled.contains(opc) =>
          getState.flatMap(state => abort(OpcodeDisabled(opc, state)))

        case OP_EQUAL =>
          onOpEqual()

        case OP_EQUALVERIFY =>
          for {
            result <- onOpEqual()
            state <- getState
            _ <- setState(state.copy(currentScript = OP_VERIFY +: state.currentScript))
          } yield result
      }
    }

    private def onOpEqual(): InterpreterContext[Option[Boolean]] = {
      getState.flatMap { state =>
        state.stack match {
          case first :: second :: rest =>
            val result = (first.bytes == second.bytes).option(ScriptNum(1)).getOrElse(ScriptNum(0))

            val newState = state.copy(
              stack = result +: rest,
              opCount = state.opCount + 1
            )

            setStateAndContinue(newState)
          case _ =>
            abort(InvalidStackOperation(OP_EQUAL, state))
        }
      }
    }
  }
}
