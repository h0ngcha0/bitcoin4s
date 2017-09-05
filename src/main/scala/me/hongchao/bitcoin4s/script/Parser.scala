package me.hongchao.bitcoin4s.script

import me.hongchao.bitcoin4s.Utils._
import me.hongchao.bitcoin4s.script.ConstantOp._

class Parser {
  def parse(bytes: Seq[Byte]): Seq[ScriptElement] = {
    ???
  }

  private def parse(bytes: Seq[Byte], acc: Seq[ScriptElement]): Seq[ScriptElement] = bytes match {
    case Nil => acc
    case head :: tail =>
      val opCode = OpCodes.all.find(_.hex == head.toHex).getOrElse {
        // FIXME: better exception
        throw new RuntimeException(s"No opcode found: $bytes")
      }

      opCode match {
        case OP_PUSHDATA(value) =>
        case OP_PUSHDATA1 =>
        case OP_PUSHDATA2 =>
        case OP_PUSHDATA4 =>
        case otherOpCode =>
      }

      Seq()
  }
}
