package me.hongchao.bitcoin4s.script

sealed trait StackOp extends ScriptOpCode

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

  implicit val interpreter = new Interpreter[StackOp] {
    def interpret(opCode: StackOp, context: InterpreterContext): InterpreterContext = {
      val stack = context.stack
      val altStack = context.altStack
      val opCount = context.opCount

      opCode match {
        case OP_DUP =>
          val stackHead = stack.headOption.getOrElse(throw new NotEnoughElementsInStack(OP_DUP, stack))

          context.copy(
            stack = stackHead +: stack,
            opCount = opCount + 1
          )

        case OP_TOALTSTACK =>
          val stackHead = stack.headOption.getOrElse(throw new NotEnoughElementsInStack(OP_TOALTSTACK, stack))

          context.copy(
            stack = stack.tail,
            altStack = stackHead +: altStack
          )

        case OP_FROMALTSTACK =>
          val altStackHead = altStack.headOption.getOrElse(throw new NotEnoughElementsInAltStack(OP_FROMALTSTACK, stack))

          context.copy(
            stack = altStackHead +: stack,
            altStack = altStack.tail
          )

        case OP_DROP =>
          val updatedStack = stack match {
            case _ :: rest =>
              rest
            case _ =>
              throw new NotEnoughElementsInStack(OP_DROP, stack)
          }

          context.copy(stack = updatedStack, opCount = opCount + 1)

        case OP_2DROP =>
          val updatedStack = stack match {
            case _ :: _ :: rest =>
              rest
            case _ =>
              throw new NotEnoughElementsInStack(OP_2DROP, stack)
          }

          context.copy(stack = updatedStack, opCount = opCount + 1)

        case OP_2DUP =>
          val updatedStack = stack match {
            case first :: second :: rest =>
              first :: second :: first :: second :: rest
            case _ =>
              throw new NotEnoughElementsInStack(OP_2DUP, stack)
          }

          context.copy(stack = updatedStack, opCount = opCount + 1)

        case OP_3DUP =>
          val updatedStack = stack match {
            case first :: second :: third :: rest =>
              first :: second :: third :: first :: second :: third :: rest
            case _ =>
              throw new NotEnoughElementsInStack(OP_3DUP, stack)
          }

          context.copy(stack = updatedStack, opCount = opCount + 1)

        case OP_OVER =>
          val updatedStack = stack match {
            case first :: second :: rest =>
              second :: first :: second :: rest
            case _ =>
              throw new NotEnoughElementsInStack(OP_OVER, stack)
          }

          context.copy(stack = updatedStack, opCount = opCount + 1)

        case OP_2OVER =>
          val updatedStack = stack match {
            case first :: second :: third :: fourth :: rest =>
              third :: fourth :: first :: second :: third :: fourth :: rest
            case _ =>
              throw new NotEnoughElementsInStack(OP_2OVER, stack)
          }

          context.copy(stack = updatedStack, opCount = opCount + 1)

        case OP_ROT =>
          val updatedStack = stack match {
            case first :: second :: third :: rest =>
              third :: first :: second :: third :: rest
            case _ =>
              throw new NotEnoughElementsInStack(OP_ROT, stack)
          }

          context.copy(stack = updatedStack, opCount = opCount + 1)

        case OP_2ROT =>
          val updatedStack = stack match {
            case first :: second :: third :: fourth :: fifth :: sixth :: rest =>
              fifth :: sixth :: first :: second :: third :: fourth :: rest
            case _ =>
              throw new NotEnoughElementsInStack(OP_2ROT, stack)
          }

          context.copy(stack = updatedStack, opCount = opCount + 1)

        case OP_SWAP =>
          val updatedStack = stack match {
            case first :: second :: rest =>
              second :: first :: rest
            case _ =>
              throw new NotEnoughElementsInStack(OP_SWAP, stack)
          }

          context.copy(stack = updatedStack, opCount = opCount + 1)

        case OP_2SWAP =>
          val updatedStack = stack match {
            case first :: second :: third :: fourth :: rest =>
              third :: fourth :: first :: second :: rest
            case _ =>
              throw new NotEnoughElementsInStack(OP_2SWAP, stack)
          }

          context.copy(stack = updatedStack, opCount = opCount + 1)

        case OP_IFDUP =>
          val updatedStack = stack match {
            case ScriptNumber(0) :: rest =>
              ScriptNumber(0) :: ScriptNumber(0) :: rest
            case first :: rest =>
              first :: rest
            case _ =>
              throw new NotEnoughElementsInStack(OP_IFDUP, stack)
          }

          context.copy(stack = updatedStack, opCount = opCount + 1)

        case OP_DEPTH =>
          val updatedStack = ScriptNumber(stack.length) +: stack
          context.copy(stack = updatedStack, opCount = opCount + 1)

        case OP_NIP =>
          val updatedStack = stack match {
            case first :: second :: rest =>
              first :: rest
            case _ =>
              throw new NotEnoughElementsInStack(OP_NIP, stack)
          }

          context.copy(stack = updatedStack, opCount = opCount + 1)

        case OP_PICK =>
          val updatedStack = stack match {
            case ScriptNumber(n) :: rest if rest.length >= n =>
              rest(n.toInt) :: rest
            case ScriptNumber(n) :: rest if rest.length < n =>
              throw new NotEnoughElementsInStack(OP_PICK, stack)
            case _ :: rest =>
              throw new NumberElementRequired(OP_PICK, stack)
            case _ =>
              throw new NotEnoughElementsInStack(OP_PICK, stack)
          }

          context.copy(stack = updatedStack, opCount = opCount + 1)

        case OP_ROLL =>
          val updatedStack = stack match {
            case ScriptNumber(n) :: rest if rest.length >= n =>
              rest(n.toInt) :: (rest.take(n.toInt) ++ rest.drop(n.toInt+1))
            case ScriptNumber(n) :: rest if rest.length < n =>
              throw new NotEnoughElementsInStack(OP_ROLL, stack)
            case _ :: rest =>
              throw new NumberElementRequired(OP_ROLL, stack)
            case _ =>
              throw new NotEnoughElementsInStack(OP_ROLL, stack)
          }

          context.copy(stack = updatedStack, opCount = opCount + 1)

        case OP_TUCK =>
          val updatedStack = stack match {
            case first :: second :: rest =>
              first :: second :: first :: rest
            case _ =>
              throw new NotEnoughElementsInStack(OP_TUCK, stack)
          }

          context.copy(stack = updatedStack, opCount = opCount + 1)
      }
    }
  }
}
