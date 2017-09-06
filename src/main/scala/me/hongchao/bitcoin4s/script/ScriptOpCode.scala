package me.hongchao.bitcoin4s.script

import me.hongchao.bitcoin4s.Utils._
// Reference: https://en.bitcoin.it/wiki/Script

sealed trait ScriptElement extends Product {
  val bytes: Seq[Byte]
}


trait ScriptConstant extends ScriptElement
case class ScriptBytes(bytes: Seq[Byte]) extends ScriptConstant

trait ScriptOpCode extends ScriptElement {
  val value: Long
  val hex: String = value.toHex
  val name: String = productPrefix
  override val bytes = Seq() // FIXME: decode hex
}

object OpCodes {
  val all =
    ArithmeticOp.all ++
    BitwiseLogicOp.all ++
    ConstantOp.all ++
    CryptoOp.all ++
    FlowControlOp.all ++
    LocktimeOp.all ++
    PseudoOp.all ++
    ReservedOp.all ++
    SpliceOp.all ++
    StackOp.all
}
