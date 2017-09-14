package me.hongchao.bitcoin4s.script

import me.hongchao.bitcoin4s.script.InterpreterError._

sealed trait ConstantOp extends ScriptOpCode

object ConstantOp {
  case object OP_FALSE extends ConstantOp { val value = 0 }
  case object OP_0 extends ConstantOp { val value = 0 }
  case object OP_PUSHDATA1 extends ConstantOp { val value = 76 }
  case object OP_PUSHDATA2 extends ConstantOp { val value = 77 }
  case object OP_PUSHDATA4 extends ConstantOp { val value = 78 }
  case object OP_1NEGATE extends ConstantOp { val value = 79 }
  case object OP_1 extends ConstantOp { val value = 81 }
  case object OP_TRUE extends ConstantOp { val value = 81 }
  case object OP_2 extends ConstantOp { val value = 82 }
  case object OP_3 extends ConstantOp { val value = 83 }
  case object OP_4 extends ConstantOp { val value = 84 }
  case object OP_5 extends ConstantOp { val value = 85}
  case object OP_6 extends ConstantOp { val value = 86 }
  case object OP_7 extends ConstantOp { val value = 87 }
  case object OP_8 extends ConstantOp { val value = 88 }
  case object OP_9 extends ConstantOp { val value = 89 }
  case object OP_10 extends ConstantOp { val value = 90 }
  case object OP_11 extends ConstantOp { val value = 91 }
  case object OP_12 extends ConstantOp { val value = 92 }
  case object OP_13 extends ConstantOp { val value = 93 }
  case object OP_14 extends ConstantOp { val value = 94 }
  case object OP_15 extends ConstantOp { val value = 95 }
  case object OP_16 extends ConstantOp { val value = 96 }

  // OpCode 1-75: The next opcode bytes is data to be pushed onto the stack
  // Reference: https://en.bitcoin.it/wiki/Script
  case class OP_PUSHDATA(val value: Long) extends ConstantOp
  val ops_pushdata = for (i <- 1 to 75) yield OP_PUSHDATA(i)

  val all = Seq(
    OP_FALSE, OP_0, OP_PUSHDATA1, OP_PUSHDATA2, OP_PUSHDATA4, OP_1NEGATE,
    OP_1, OP_TRUE, OP_2, OP_3, OP_4, OP_5, OP_6, OP_7, OP_8, OP_9, OP_10,
    OP_11, OP_12, OP_13, OP_14, OP_15, OP_16
  ) ++ ops_pushdata

  // https://github.com/bitcoin/bips/blob/master/bip-0062.mediawiki#numbers
  // How does this work?

  implicit val interpreter = new Interpreter[ConstantOp] {
    def interpret(opCode: ConstantOp, context: InterpreterState): InterpreterState = {
      opCode match {
        case opc if opc.value >= 79 && opc.value <= 96 =>
          // from OP_1NEGATE to OP_16
          context.copy(
            script = context.script.tail,
            stack = ScriptNum(opc.value - 80) +: context.stack,
            opCount = context.opCount + 1
          )

        case OP_0 | OP_FALSE =>
          // Push empty byte array to the stack
          context.copy(
            script = context.script.tail,
            stack = ScriptConstant(Seq.empty[Byte]) +: context.stack,
            opCount = context.opCount + 1
          )

        case _: OP_PUSHDATA | OP_PUSHDATA1 | OP_PUSHDATA2 | OP_PUSHDATA4 =>
          context.script match {
            case `opCode` :: (dataToPush: ScriptConstant) :: rest =>
              context.copy(
                script = rest,
                stack = dataToPush +: context.stack,
                opCount = context.opCount + 1
              )
            case `opCode` :: _ :: _ =>
              throw new OperantMustBeScriptConstant(opCode, context.stack)
            case _ =>
              throw new NotEnoughElementsInStack(opCode, context.stack)
          }
      }
    }
  }
}
