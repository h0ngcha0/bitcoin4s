package me.hongchao.bitcoin4s.script

import me.hongchao.bitcoin4s.script.ScriptFlag.SCRIPT_VERIFY_MINIMALDATA
import me.hongchao.bitcoin4s.Utils._
import me.hongchao.bitcoin4s.script.FlowControlOp.OP_VERIFY
import me.hongchao.bitcoin4s.script.InterpreterError._
import cats.implicits._

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

  val disabled = Seq(OP_2MUL, OP_2DIV, OP_MOD, OP_LSHIFT, OP_RSHIFT)

  implicit val interpreter = new Interpreter[ArithmeticOp] {
    def interpret(opCode: ArithmeticOp, context: InterpreterContext): InterpreterContext = {
      val requireMinimalEncoding: Boolean = context.flags.contains(SCRIPT_VERIFY_MINIMALDATA)

      opCode match {
        case opc if disabled.contains(opc) =>
          throw new OpcodeDisabled(opc, context.stack)

        case OP_1ADD =>
          oneOperant(opCode, context, (number: ScriptNum) => number + 1)

        case OP_1SUB =>
          oneOperant(opCode, context, (number: ScriptNum) => number - 1)

        case OP_NEGATE =>
          oneOperant(opCode, context, (number: ScriptNum) => ScriptNum(-number.value))

        case OP_ABS =>
          oneOperant(opCode, context, (number: ScriptNum) => ScriptNum(Math.abs(number.value)))

        case OP_NOT =>
          oneOperant(opCode, context, (number: ScriptNum) => {
            (number.value == 0).option(ScriptNum(1)).getOrElse(ScriptNum(0))
          })

        case OP_0NOTEQUAL =>
          oneOperant(opCode, context, (number: ScriptNum) => {
            (number.value == 0).option(ScriptNum(0)).getOrElse(ScriptNum(1))
          })

        case OP_ADD =>
          twoOperants(opCode, context, (first: ScriptNum, second: ScriptNum) => first + second)

        case OP_SUB =>
          twoOperants(opCode, context, (first: ScriptNum, second: ScriptNum) => first - second)

        case OP_BOOLAND =>
          twoOperants(opCode, context, (first: ScriptNum, second: ScriptNum) => {
            (first.value =!= 0 && second.value =!= 0).option(ScriptNum(1)).getOrElse(ScriptNum(0))
          })

        case OP_BOOLOR =>
          twoOperants(opCode, context, (first: ScriptNum, second: ScriptNum) => {
            (first.value =!= 0 || second.value =!= 0).option(ScriptNum(1)).getOrElse(ScriptNum(0))
          })

        case OP_NUMEQUAL =>
          twoOperants(opCode, context, (first: ScriptNum, second: ScriptNum) => {
            (first.value === second.value).option(ScriptNum(1)).getOrElse(ScriptNum(0))
          })

        case OP_NUMEQUALVERIFY =>
          // OP_NUMEQUAL + OP_VERIFY
          val updatedContext = twoOperants(opCode, context, (first: ScriptNum, second: ScriptNum) => {
            (first.value === second.value).option(ScriptNum(1)).getOrElse(ScriptNum(0))
          })

          updatedContext.copy(script = OP_VERIFY +: updatedContext.script)

        case OP_NUMNOTEQUAL =>
          twoOperants(opCode, context, (first: ScriptNum, second: ScriptNum) => {
            (first.value =!= second.value).option(ScriptNum(1)).getOrElse(ScriptNum(0))
          })

        case OP_LESSTHAN =>
          twoOperants(opCode, context, (first: ScriptNum, second: ScriptNum) => {
            (first < second).option(ScriptNum(1)).getOrElse(ScriptNum(0))
          })

        case OP_GREATERTHAN =>
          twoOperants(opCode, context, (first: ScriptNum, second: ScriptNum) => {
            (first > second).option(ScriptNum(1)).getOrElse(ScriptNum(0))
          })

        case OP_GREATERTHANOREQUAL =>
          twoOperants(opCode, context, (first: ScriptNum, second: ScriptNum) => {
            (first >= second).option(ScriptNum(1)).getOrElse(ScriptNum(0))
          })

        case OP_MIN =>
          twoOperants(opCode, context, (first: ScriptNum, second: ScriptNum) => {
            ScriptNum(Math.min(first.value, second.value))
          })

        case OP_MAX =>
          twoOperants(opCode, context, (first: ScriptNum, second: ScriptNum) => {
            ScriptNum(Math.max(first.value, second.value))
          })

        case OP_WITHIN =>
          context.stack match {
            case (first: ScriptConstant) :: (second: ScriptConstant) :: (third: ScriptConstant) :: rest =>
              val firstNumber = ScriptNum(first.bytes, requireMinimalEncoding)
              val secondNumber = ScriptNum(second.bytes, requireMinimalEncoding)
              val thirdNumber = ScriptNum(third.bytes, requireMinimalEncoding)
              val isWithin = (thirdNumber < firstNumber && thirdNumber > secondNumber)

              context.copy(
                script = context.script.tail,
                stack = isWithin.option(ScriptNum(1)).getOrElse(ScriptNum(0)) +: rest,
                opCount = context.opCount + 1
              )
            case _ :: _ :: _ :: _ =>
              throw NotAllOperantsAreConstant(opCode, context.stack)
            case _ =>
              throw NotEnoughElementsInStack(opCode, context.stack)
          }
      }
    }

    private def oneOperant(opCode: ArithmeticOp, context: InterpreterContext, convert: (ScriptNum) => ScriptNum): InterpreterContext = {
      val requireMinimalEncoding: Boolean = context.flags.contains(SCRIPT_VERIFY_MINIMALDATA)
      context.stack match {
        case (first: ScriptConstant) :: rest =>
          val firstNumber = ScriptNum(first.bytes, requireMinimalEncoding)
          context.copy(
            script = context.script.tail,
            stack = convert(firstNumber) +: rest,
            opCount = context.opCount + 1
          )
        case _ :: _ =>
          throw NotAllOperantsAreConstant(opCode, context.stack)
        case _ =>
          throw NotEnoughElementsInStack(opCode, context.stack)
      }
    }

    private def twoOperants(opCode: ArithmeticOp, context: InterpreterContext, convert: (ScriptNum, ScriptNum) => ScriptNum): InterpreterContext = {
      val requireMinimalEncoding: Boolean = context.flags.contains(SCRIPT_VERIFY_MINIMALDATA)
      context.stack match {
        case (first: ScriptConstant) :: (second: ScriptConstant) :: rest =>
          val firstNumber = ScriptNum(first.bytes, requireMinimalEncoding)
          val secondNumber = ScriptNum(second.bytes, requireMinimalEncoding)
          context.copy(
            script = context.script.tail,
            stack = convert(firstNumber, secondNumber) +: rest,
            opCount = context.opCount + 1
          )
        case _ :: _ :: _ =>
          throw NotAllOperantsAreConstant(opCode, context.stack)
        case _ =>
          throw NotEnoughElementsInStack(opCode, context.stack)
      }

    }
  }
}
