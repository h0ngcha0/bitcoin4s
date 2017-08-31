package me.hongchao.bitcoin4s.script

import simulacrum._

case class InterpreterContext(
  script: Seq[OpCode],
  stack: Seq[OpCode],
  altStack: Seq[OpCode],
  opCount: Int
)

sealed trait InterpreterError extends RuntimeException {
  val opCode: OpCode
  val stack: Seq[OpCode]
  val description: String

  super.initCause(new Throwable(s"$description\nopCode: $opCode\nstack: $stack"))
}

case class NotEnoughElementsInStack(opCode: OpCode, stack: Seq[OpCode]) extends InterpreterError {
  val description = "Not enough elements in the stack"
}

case class NotEnoughElementsInAltStack(opCode: OpCode, stack: Seq[OpCode]) extends InterpreterError {
  val description = "Not enough elements in the alternative stack"
}


@typeclass trait Interpreter[A <: OpCode] {
  def interpret(opCode: A, context: InterpreterContext): InterpreterContext
}