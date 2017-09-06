package me.hongchao.bitcoin4s.script

import simulacrum._

case class InterpreterContext(
  script: Seq[ScriptElement],
  stack: Seq[ScriptElement],
  altStack: Seq[ScriptElement],
  flags: Seq[ScriptFlag],
  opCount: Int
)


sealed trait InterpreterError extends RuntimeException {
  val opCode: ScriptOpCode
  val stack: Seq[ScriptElement]
  val description: String

  super.initCause(new Throwable(s"$description\nopCode: $opCode\nstack: $stack"))
}

case class NotEnoughElementsInStack(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
  val description = "Not enough elements in the stack"
}

case class NotEnoughElementsInAltStack(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
  val description = "Not enough elements in the alternative stack"
}

case class NotAllOperantsAreConstant(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
  val description = "Not all operants are constant"
}

case class OperantMustBeScriptNum(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
  val description = "Operant must be ScriptNum"
}

case class OpcodeDisabled(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
  val description = "Opcode is disabled"
}


@typeclass trait Interpreter[A <: ScriptOpCode] {
  // def interpret[A](opCode: A): State[InterpreterContext, Either[String, Boolean]]
  def interpret(opCode: A, context: InterpreterContext): InterpreterContext
}