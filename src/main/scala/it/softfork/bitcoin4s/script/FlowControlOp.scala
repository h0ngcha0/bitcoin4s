package it.softfork.bitcoin4s.script

import scala.annotation.tailrec
import it.softfork.bitcoin4s.Utils._
import it.softfork.bitcoin4s.script.Interpreter._
import it.softfork.bitcoin4s.script.InterpreterError._
import tech.minna.utilities.TraitEnumeration
import it.softfork.bitcoin4s.script.ScriptExecutionStage.ExecutingScriptWitness
import cats.implicits._

import scala.util.{Failure, Success, Try}

sealed trait FlowControlOp extends ScriptOpCode

object FlowControlOp {
  case object OP_NOP extends FlowControlOp { val value = 97 }
  case object OP_IF extends FlowControlOp { val value = 99 }
  case object OP_NOTIF extends FlowControlOp { val value = 100 }
  case object OP_ELSE extends FlowControlOp { val value = 103 }
  case object OP_ENDIF extends FlowControlOp { val value = 104 }
  case object OP_VERIFY extends FlowControlOp { val value = 105 }
  case object OP_RETURN extends FlowControlOp { val value = 106 }

  val all = TraitEnumeration.values[FlowControlOp]

  implicit val interpreter = new InterpretableOp[FlowControlOp] {

    def interpret(opCode: FlowControlOp): InterpreterContext[Option[Boolean]] = {
      getState.flatMap { state =>
        opCode match {
          case OP_NOP =>
            setStateAndContinue(state.copy(opCount = state.opCount + 1))

          case OP_IF | OP_NOTIF =>
            Try {
              splitScriptOnConditional(
                script = state.currentScript,
                nestedDepth = 1,
                acc = ConditionalBranchSplitResult()
              )
            } match {
              case Success(ConditionalBranchSplitResult(branches, rest)) =>
                state.stack match {
                  case first :: tail =>
                    val firstNumber = ScriptNum(first.bytes, state.ScriptFlags.requireMinimalEncoding)
                    val positiveBranches = branches.zipWithIndex.filter(_._2 % 2 == 0).map(_._1)
                    val negativeBranches = branches.zipWithIndex.filter(_._2 % 2 == 1).map(_._1)

                    val isP2WSH = state.scriptExecutionStage == ExecutingScriptWitness
                    val stackTopMinimal = first.bytes == Seq.empty || first.bytes == Seq(1.byteValue)

                    if (isP2WSH && state.flags.contains(ScriptFlag.SCRIPT_VERIFY_MINIMALIF) && !stackTopMinimal) {
                      abort(MinimalIf(opCode, state))
                    } else {
                      val pickNegativeBranches = firstNumber == 0 && opCode == OP_IF || firstNumber != 0 && opCode == OP_NOTIF
                      val updatedScript = pickNegativeBranches
                        .option(negativeBranches.flatten ++ rest)
                        .getOrElse(positiveBranches.flatten ++ rest)

                      // Even if other branch is not executed, need to take it into consideration
                      // when calculating opCodes
                      val otherBranchOpCount: Seq[ScriptElement] = pickNegativeBranches
                        .option(positiveBranches.flatten.filter(OpCodes.isNonReservedOpCode))
                        .getOrElse(negativeBranches.flatten.filter(OpCodes.isNonReservedOpCode))

                      // numberOfBranches should be added to the opCode as well since it requires either a OP_ELSE
                      // or OP_ENDIF to create a new branch.
                      val numberOfBranches = branches.length

                      val updatedState = state.copy(
                        currentScript = updatedScript,
                        stack = tail,
                        opCount = state.opCount + otherBranchOpCount.length + numberOfBranches + 1
                      )

                      setStateAndContinue(updatedState)
                    }

                  case _ =>
                    abort(UnbalancedConditional(opCode, state))
                }

              case Failure(error: InterpreterError) =>
                abort(error)

              case Failure(e @ _) =>
                abort(UnbalancedConditional(opCode, state))
            }

          case OP_ELSE | OP_ENDIF =>
            abort(UnbalancedConditional(opCode, state))

          case OP_VERIFY =>
            state.stack match {
              case first :: tail =>
                val firstNumber = ScriptNum(first.bytes, state.ScriptFlags.requireMinimalEncoding, first.bytes.size)
                if (firstNumber == 0) {
                  abort(VerificationFailed(OP_VERIFY, state))
                } else {
                  val newState = state.copy(
                    stack = tail,
                    opCount = state.opCount + 1
                  )
                  setStateAndContinue(newState)
                }
              case _ =>
                abort(InvalidStackOperation(OP_VERIFY, state))
            }

          case OP_RETURN =>
            abort(FoundOpReturn(opCode, state))
        }
      }
    }
  }

  // There could be mulitple of OP_ELSE, e.g.:
  // ["1", "IF 1 ELSE 0 ELSE 1 ENDIF ADD 2 EQUAL", "P2SH,STRICTENC", "OK"]
  // Each of the OP_ELSE could be executed depending on if the previous branch is executed
  private case class ConditionalBranchSplitResult(
    branches: Seq[Seq[ScriptElement]] = Seq(Seq.empty),
    rest: Seq[ScriptElement] = Seq.empty
  )

  @tailrec
  private def splitScriptOnConditional(
    script: Seq[ScriptElement],
    nestedDepth: Int,
    acc: ConditionalBranchSplitResult
  ): ConditionalBranchSplitResult = {
    script match {
      case Nil =>
        throw new RuntimeException("Unbalanced conditionals")
      case opCode :: tail if opCode == OP_IF || opCode == OP_NOTIF =>
        val newNestedDepth = nestedDepth + 1

        if (newNestedDepth > 1) {
          splitScriptOnConditional(
            script = tail,
            nestedDepth = newNestedDepth,
            acc = pushToBranch(opCode, acc)
          )
        } else {
          throw new RuntimeException("Unbalanced conditionals")
        }

      case OP_ENDIF :: tail =>
        if (nestedDepth == 1) {
          acc.copy(rest = tail)
        } else if (nestedDepth > 1) {
          splitScriptOnConditional(
            script = tail,
            nestedDepth = nestedDepth - 1,
            acc = pushToBranch(OP_ENDIF, acc)
          )
        } else {
          throw new RuntimeException("Unbalanced conditionals")
        }

      case OP_ELSE :: tail =>
        if (nestedDepth == 1) {
          splitScriptOnConditional(
            script = tail,
            nestedDepth = nestedDepth,
            acc = acc.copy(branches = acc.branches :+ Seq.empty) // add a new empty branch
          )
        } else if (nestedDepth > 1) {
          splitScriptOnConditional(
            script = tail,
            nestedDepth = nestedDepth,
            acc = pushToBranch(OP_ELSE, acc)
          )
        } else {
          throw new RuntimeException("Unbalanced conditionals")
        }

      case element :: tail if nestedDepth >= 1 =>
        splitScriptOnConditional(
          script = tail,
          nestedDepth = nestedDepth,
          acc = pushToBranch(element, acc)
        )
    }
  }

  private def pushToBranch(
    element: ScriptElement,
    currentResult: ConditionalBranchSplitResult
  ): ConditionalBranchSplitResult = {
    val currentBranches = currentResult.branches
    val currentBranch = currentBranches.last
    val updatedCurrentBranch = currentBranch :+ element
    val updatedCurrentBranches = currentBranches.dropRight(1) :+ updatedCurrentBranch

    currentResult.copy(branches = updatedCurrentBranches)
  }
}
