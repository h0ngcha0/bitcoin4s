package me.hongchao.bitcoin4s.script

sealed trait FlowControlOp extends ScriptOpCode

object FlowControlOp {
  case object OP_NOP extends FlowControlOp { val value = 97 }
  case object OP_IF extends FlowControlOp { val value = 99 }
  case object OP_NOTIF extends FlowControlOp { val value = 100 }
  case object OP_ELSE extends FlowControlOp { val value = 103 }
  case object OP_ENDIF extends FlowControlOp { val value = 104 }
  case object OP_VERIFY extends FlowControlOp { val value = 105 }
  case object OP_RETURN extends FlowControlOp { val value = 106 }

  val all = Seq(OP_NOP, OP_IF, OP_NOTIF, OP_ELSE, OP_ENDIF, OP_VERIFY, OP_RETURN)
}
