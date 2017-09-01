package me.hongchao.bitcoin4s.script

sealed trait SpliceOp extends ScriptOpCode

case object OP_CAT extends SpliceOp { val value = 126 }
case object OP_SUBSTR extends SpliceOp { val value = 127 }
case object OP_LEFT extends SpliceOp { val value = 128 }
case object OP_RIGHT extends SpliceOp { val value = 129 }
case object OP_SIZE extends SpliceOp { val value = 130 }

object SpliceOps {
  val all = Seq(OP_CAT, OP_SUBSTR, OP_LEFT, OP_RIGHT, OP_SIZE)
}