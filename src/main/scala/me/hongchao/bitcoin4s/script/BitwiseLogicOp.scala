package me.hongchao.bitcoin4s.script

import me.hongchao.bitcoin4s.Utils._
import me.hongchao.bitcoin4s.script.FlowControlOp.OP_VERIFY
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

  implicit val interpreter = new Interpreter[BitwiseLogicOp] {
    def interpret(opCode: BitwiseLogicOp, context: InterpreterState): InterpreterState = {
      opCode match {
        case opc if disabled.contains(opc) =>
          throw new OpcodeDisabled(opc, context.stack)

        case OP_EQUAL =>
          onOpEqual(context)

        case OP_EQUALVERIFY =>
          val updatedContext = onOpEqual(context)
          updatedContext.copy(script = OP_VERIFY +: updatedContext.script)
      }
    }

    private def onOpEqual(context: InterpreterState): InterpreterState = {
      context.stack match {
        case first :: second :: rest =>
          val result = (first.bytes == second.bytes).option(ScriptNum(1)).getOrElse(ScriptNum(0))

          context.copy(
            script = context.script.tail,
            stack = result +: rest,
            opCount = context.opCount + 1
          )

        case _ =>
          throw NotEnoughElementsInStack(OP_EQUAL, context.stack)
      }
    }
  }
}