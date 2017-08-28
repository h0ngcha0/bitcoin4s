package me.hongchao.bitcoin4s.script

sealed trait LocktimeOp extends OpCode

case object OP_CHECKLOCKTIMEVERIFY extends LocktimeOp { val value = 177 }
case object OP_CHECKSEQUENCEVERIFY extends LocktimeOp { val value = 178 }
case object OP_NOP2 extends LocktimeOp { val value = 177 }
case object OP_NOP3 extends LocktimeOp { val value = 178 }

object LocktimeOps {
  val all = Seq(OP_CHECKMULTISIGVERIFY, OP_CHECKSEQUENCEVERIFY, OP_NOP2, OP_NOP3)
}