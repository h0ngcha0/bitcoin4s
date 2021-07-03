package it.softfork.bitcoin4s.script

import it.softfork.bitcoin4s.script.Interpreter._
import it.softfork.bitcoin4s.script.InterpreterError._
import cats.implicits._

sealed trait ConstantOp extends ScriptOpCode {
  override def toString: String = name
}

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
  case object OP_5 extends ConstantOp { val value = 85 }
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
  case class OP_PUSHDATA(val value: Long) extends ConstantOp {
    override def toString: String = s"OP_PUSHDATA($value)"
  }
  val ops_pushdata = for (i <- 1 to 75) yield OP_PUSHDATA(i)

  val all = Seq(
    OP_0,
    OP_FALSE,
    OP_PUSHDATA1,
    OP_PUSHDATA2,
    OP_PUSHDATA4,
    OP_1NEGATE,
    OP_1,
    OP_TRUE,
    OP_2,
    OP_3,
    OP_4,
    OP_5,
    OP_6,
    OP_7,
    OP_8,
    OP_9,
    OP_10,
    OP_11,
    OP_12,
    OP_13,
    OP_14,
    OP_15,
    OP_16
  ) ++ ops_pushdata

  // https://github.com/bitcoin/bips/blob/master/bip-0062.mediawiki#numbers
  // How does this work?

  implicit val interpreter = new InterpretableOp[ConstantOp] {

    override def interpret(opCode: ConstantOp): InterpreterContext[Option[Boolean]] = {
      (opCode: @unchecked) match {
        case opc if opc.value >= 79 && opc.value <= 96 => // From OP_1NEGATE to OP_16
          getState
            .flatMap { state =>
              // from OP_1NEGATE to OP_16
              setStateAndContinue(
                state.copy(
                  stack = ScriptNum(opc.value - 80) +: state.stack
                )
              )
            }

        case OP_0 | OP_FALSE =>
          getState
            .flatMap { state =>
              // Push empty byte array to the stack
              setStateAndContinue(
                state.copy(
                  stack = ScriptConstant(Array.empty[Byte]) +: state.stack
                )
              )
            }

        case _: OP_PUSHDATA | OP_PUSHDATA1 | OP_PUSHDATA2 | OP_PUSHDATA4 =>
          getState
            .flatMap { state =>
              state.currentScript match {
                case (dataToPush: ScriptConstant) :: rest =>
                  if (!checkMinimalPush(opCode, dataToPush) && state.ScriptFlags.requireMinimalEncoding()) {
                    abort(NotMinimalEncoding(opCode, state))
                  } else {
                    setStateAndContinue(
                      state.copy(
                        currentScript = rest,
                        stack = dataToPush +: state.stack
                      )
                    )
                  }
                case OP_0 :: rest =>
                  if (state.ScriptFlags.requireMinimalEncoding()) {
                    abort(NotMinimalEncoding(opCode, state))
                  } else {
                    setStateAndContinue(
                      state.copy(
                        currentScript = rest,
                        stack = ScriptNum(0) +: state.stack
                      )
                    )
                  }
                case _ :: _ =>
                  abort(OperantMustBeScriptConstant(opCode, state))
                case _ =>
                  abort(NotEnoughElementsInStack(opCode, state))
              }
            }
      }
    }
  }

  // Similiar to https://github.com/bitcoin/bitcoin/blob/923c5e93a90a6eddde3ab8589fc393da95bbc489/src/script/interpreter.cpp#L215
  private def checkMinimalPush(opCode: ScriptOpCode, constant: ScriptConstant): Boolean = {
    constant.bytes.size match {
      case 0 =>
        opCode == OP_0
      case 1 if constant.bytes(0) == 0x81.toByte =>
        opCode == OP_1NEGATE
      case 1 if constant.bytes(0) >= 1 && constant.bytes(0) <= 16 =>
        all.find(_.value == (OP_1.value + constant.bytes(0) - 1)).exists(_ == opCode)
      case s if s <= 75 =>
        opCode == OP_PUSHDATA(s)
      case s if s <= 255 =>
        opCode == OP_PUSHDATA1
      case s if s <= 65535 =>
        opCode == OP_PUSHDATA2
      case _ =>
        true
    }
  }
}
