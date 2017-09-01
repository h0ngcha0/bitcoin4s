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
    ArithmeticOps.all ++
    BitwiseLogicOps.all ++
    ConstantOps.all ++
    CryptoOps.all ++
    FlowControlOps.all ++
    LocktimeOps.all ++
    PseudoOps.all ++
    ReservedOps.all ++
    SpliceOps.all ++
    StackOps.all
}
