package me.hongchao.bitcoin4s.script

sealed trait ArithmeticOp extends ScriptOpCode

object ArithmeticOp {
  case object OP_1ADD extends ArithmeticOp { val value = 139 }
  case object OP_1SUB extends ArithmeticOp { val value = 140 }
  case object OP_2MUL extends ArithmeticOp { val value = 141 }
  case object OP_2DIV extends ArithmeticOp { val value = 142 }
  case object OP_NEGATE extends ArithmeticOp { val value = 143 }
  case object OP_ABS extends ArithmeticOp { val value = 144 }
  case object OP_NOT extends ArithmeticOp { val value = 145 }
  case object OP_0NOTEQUAL extends ArithmeticOp { val value = 146 }
  case object OP_ADD extends ArithmeticOp { val value = 147 }
  case object OP_SUB extends ArithmeticOp { val value = 148 }
  case object OP_MUL extends ArithmeticOp { val value = 149 }
  case object OP_DIV extends ArithmeticOp { val value = 150 }
  case object OP_MOD extends ArithmeticOp { val value = 151 }
  case object OP_LSHIFT extends ArithmeticOp { val value = 152 }
  case object OP_RSHIFT extends ArithmeticOp { val value = 153 }
  case object OP_BOOLAND extends ArithmeticOp { val value = 154 }
  case object OP_BOOLOR extends ArithmeticOp { val value = 155 }
  case object OP_NUMEQUAL extends ArithmeticOp { val value = 156 }
  case object OP_NUMEQUALVERIFY extends ArithmeticOp { val value = 157 }
  case object OP_NUMNOTEQUAL extends ArithmeticOp { val value = 158 }
  case object OP_LESSTHAN extends ArithmeticOp { val value = 159 }
  case object OP_GREATERTHAN extends ArithmeticOp { val value = 160 }
  case object OP_LESSTHANOREQUAL extends ArithmeticOp { val value = 161 }
  case object OP_GREATERTHANOREQUAL extends ArithmeticOp { val value = 162 }
  case object OP_MIN extends ArithmeticOp { val value = 163 }
  case object OP_MAX extends ArithmeticOp { val value = 164 }
  case object OP_WITHIN extends ArithmeticOp { val value = 165 }

  val all = Seq(
    OP_1ADD, OP_1SUB, OP_2MUL, OP_2DIV, OP_NEGATE, OP_ABS, OP_NOT, OP_0NOTEQUAL,
    OP_ADD, OP_SUB, OP_MUL, OP_DIV, OP_MOD, OP_LSHIFT, OP_RSHIFT, OP_BOOLAND,
    OP_BOOLOR, OP_NUMEQUAL, OP_NUMEQUALVERIFY, OP_NUMNOTEQUAL, OP_LESSTHAN,
    OP_GREATERTHAN, OP_LESSTHANOREQUAL, OP_GREATERTHANOREQUAL, OP_MIN, OP_MAX, OP_WITHIN
  )

  implicit val interpreter = new Interpreter[ArithmeticOp] {
    def interpret(opCode: ArithmeticOp, context: InterpreterContext): InterpreterContext = {
      val script = context.script
      val stack = context.stack
      val flags = context.flags

      opCode match {
        case OP_ADD =>
          val firstTwoElements = stack match {
            case first :: second :: _ => Seq(first, second)
            case _ => throw NotEnoughElementsInStack(OP_1ADD, stack)
          }

          context.copy(
            script = script.tail
          )
      }
    }
  }
}
