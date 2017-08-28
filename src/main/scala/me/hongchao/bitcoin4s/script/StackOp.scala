package me.hongchao.bitcoin4s.script

sealed trait StackOp extends OpCode

case object OP_TOALTSTACK extends StackOp { val value = 107 }
case object OP_FROMALTSTACK extends StackOp { val value = 108 }
case object OP_2DROP extends StackOp { val value = 109 }
case object OP_2DUP extends StackOp { val value = 110 }
case object OP_3DUP extends StackOp { val value = 111 }
case object OP_2OVER extends StackOp { val value = 112 }
case object OP_2ROT extends StackOp { val value = 113 }
case object OP_2SWAP extends StackOp { val value = 114 }
case object OP_IFDUP extends StackOp { val value = 115 }
case object OP_DEPTH extends StackOp { val value = 116 }
case object OP_DROP extends StackOp { val value = 117 }
case object OP_DUP extends StackOp { val value = 118 }
case object OP_NIP extends StackOp { val value = 119 }
case object OP_OVER extends StackOp { val value = 120 }
case object OP_PICK extends StackOp { val value = 121 }
case object OP_ROLL extends StackOp { val value = 122 }
case object OP_ROT extends StackOp { val value = 123 }
case object OP_SWAP extends StackOp { val value = 124 }
case object OP_TUCK extends StackOp { val value = 125 }

object StackOps {
  val all = Seq(
    OP_TOALTSTACK, OP_FROMALTSTACK, OP_2DROP, OP_2DUP, OP_3DUP, OP_2OVER,
    OP_2ROT, OP_2SWAP, OP_IFDUP, OP_DEPTH, OP_DROP, OP_DUP, OP_NIP, OP_OVER,
    OP_PICK, OP_ROLL, OP_ROT, OP_SWAP, OP_TUCK
  )
}
