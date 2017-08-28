package me.hongchao.bitcoin4s.script

// Reference: https://en.bitcoin.it/wiki/Script

trait OpCode extends Product {
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
