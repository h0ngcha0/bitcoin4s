package me.hongchao.bitcoin4s.script

sealed trait BitwiseLogicOp extends ScriptOpCode

case object OP_INVERT extends BitwiseLogicOp { val value = 131 }
case object OP_AND extends BitwiseLogicOp { val value = 132 }
case object OP_OR extends BitwiseLogicOp { val value = 133 }
case object OP_XOR extends BitwiseLogicOp { val value = 134 }
case object OP_EQUAL extends BitwiseLogicOp { val value = 135 }
case object OP_EQUALVERIFY extends BitwiseLogicOp { val value = 136 }

object BitwiseLogicOps {
  val all = Seq(OP_INVERT, OP_AND, OP_OR, OP_XOR, OP_EQUAL, OP_EQUALVERIFY)
}