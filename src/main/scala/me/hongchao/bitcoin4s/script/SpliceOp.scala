package me.hongchao.bitcoin4s.script

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

  implicit val interpreter = new Interpreter[SpliceOp] {
    def interpret(opCode: SpliceOp, context: InterpreterState): InterpreterState = {
      opCode match {
        case opc if disabled.contains(opc) =>
          throw new OpcodeDisabled(opc, context.stack)

        case OP_SIZE =>
          context.stack match {
            case head :: tail =>
              context.copy(
                script = context.script.tail,
                stack = ScriptNum(head.bytes.length) +: context.stack,
                opCount = context.opCount + 1
              )
            case _ =>
              throw NotEnoughElementsInStack(OP_SIZE, context.stack)
          }
      }
    }
  }
}

