package me.hongchao.bitcoin4s.script

// Reference: https://en.bitcoin.it/wiki/Script

sealed trait ScriptElement extends Product

case class ScriptNumber(value: Long) extends ScriptElement
case class ScriptString(value: String) extends ScriptElement

trait ScriptOpCode extends ScriptElement {
  val value: Int
  val hex: String = value.toHexString
  val name: String = productPrefix
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
