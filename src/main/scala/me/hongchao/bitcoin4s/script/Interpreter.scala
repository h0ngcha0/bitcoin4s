package me.hongchao.bitcoin4s.script

import io.github.yzernik.bitcoinscodec.messages.Tx
import io.github.yzernik.bitcoinscodec.structures.TxIn
import me.hongchao.bitcoin4s.script.Interpreter._
import me.hongchao.bitcoin4s.Utils._
import com.typesafe.scalalogging.StrictLogging
import simulacrum._
import cats.data.StateT
import cats.implicits._
import me.hongchao.bitcoin4s.script.ConstantOp._

sealed trait ScriptExecutionProgress
object ScriptExecutionProgress {
  case object ExecutingScriptSig extends ScriptExecutionProgress
  case object ExecutingScriptPubKey extends ScriptExecutionProgress
  case object ExecutingScriptP2SH extends ScriptExecutionProgress
}

case class InterpreterState(
  scriptPubKey: Seq[ScriptElement],
  scriptSig: Seq[ScriptElement],
  currentScript: Seq[ScriptElement],
  p2shScript: Option[Seq[ScriptElement]] = None,
  stack: Seq[ScriptElement] = Seq.empty,
  altStack: Seq[ScriptElement] = Seq.empty,
  flags: Seq[ScriptFlag],
  opCount: Int = 0,
  transaction: Tx,
  inputIndex: Int,
  sigVersion: SigVersion,
  scriptExecutionProgress: ScriptExecutionProgress = ScriptExecutionProgress.ExecutingScriptSig
) {

  // Execute one OpCode, which takes the stack top element and and produce a new
  // value. The new value it put on top of the stack
  def replaceStackTopElement(scriptElement: ScriptElement): InterpreterState = {
    copy(
      stack = scriptElement +: stack.tail,
      opCount = opCount + 1
    )
  }

  def dropTopElement(): InterpreterState = {
    copy(
      stack = stack.tail,
      opCount = opCount + 1
    )
  }

  def transactionInput: TxIn = {
    require(inputIndex >= 0 && inputIndex < transaction.tx_in.length, "Transation input index must be within range.")
    transaction.tx_in(inputIndex)
  }

  def scriptSignature: Seq[Byte] = transactionInput.sig_script.toSeq

  def cltvEnabled(): Boolean = {
    flags.contains(ScriptFlag.SCRIPT_VERIFY_CHECKLOCKTIMEVERIFY)
  }

  def csvEnabled(): Boolean = {
    flags.contains(ScriptFlag.SCRIPT_VERIFY_CHECKSEQUENCEVERIFY)
  }

  def disCourageUpgradableNop(): Boolean = {
    flags.contains(ScriptFlag.SCRIPT_VERIFY_DISCOURAGE_UPGRADABLE_NOPS)
  }

  def requireMinimalEncoding(): Boolean = {
    flags.contains(ScriptFlag.SCRIPT_VERIFY_MINIMALDATA)
  }

  def p2sh(): Boolean = {
    flags.contains(ScriptFlag.SCRIPT_VERIFY_P2SH)
  }

  def requireCleanStack(): Boolean = {
    flags.contains(ScriptFlag.SCRIPT_VERIFY_CLEANSTACK)
  }
}


sealed trait InterpreterError extends RuntimeException {
  val opCode: ScriptOpCode
  val stack: Seq[ScriptElement]
  val description: String

  super.initCause(new Throwable(s"$description\nopCode: $opCode\nstack: $stack"))
}

object InterpreterError {
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

  case class OperantMustBeScriptConstant(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Operant must be ScriptConstant"
  }

  case class OpcodeDisabled(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Opcode is disabled"
  }

  case class NotExecutableReservedOpcode(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Found not executable reserved opcode"
  }

  case class DiscourageUpgradableNops(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Found not executable reserved opcode"
  }

  case class InValidReservedOpcode(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Found executable reserved opcode that invalidates the transaction"
  }

  case class VerificationFailed(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Verification on top of the stack failed"
  }

  case class CLTVFailed(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "CheckLockTimeVerify failed"
  }

  case class CSVFailed(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "CheckSequenceVerify failed"
  }

  case class NotImplemented(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Not implemented"
  }

  case class UnbalancedCondition(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description: String = "Unbalanced condition"
  }

  case class NoSerializedScriptFound(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description: String = "No serialized script found for p2sh"
  }

  case class UnexpectedOpCode(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description: String = "Unexpected op code encountered"
  }
}

@typeclass trait Interpretable[A <: ScriptOpCode] extends StrictLogging {
  def interpret(opCode: A): InterpreterContext[Option[Boolean]]

  def interpret(opCode: A, verbose: Boolean): InterpreterContext[Option[Boolean]] = {
    if (verbose) {
      for {
        oldState <- StateT.get[InterpreterErrorHandler, InterpreterState]
        newContext <- interpret(opCode)
      } yield {
        logger.info(s"State\nscript: ${opCode +: oldState.currentScript}\nstack: ${oldState.stack}\naltstack: ${oldState.altStack}")
        logger.info("~~~~~~~~~~~~~~~~~~~~~")
        newContext
      }
    } else {
      interpret(opCode)
    }
  }
}

object Interpreter {
  type InterpreterErrorHandler[T] = Either[InterpreterError, T]
  type InterpreterContext[T] = StateT[InterpreterErrorHandler, InterpreterState, T]

  import me.hongchao.bitcoin4s.script.Interpretable.ops._

  def interpret(verbose: Boolean = false): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      state.currentScript match {
        case head :: tail =>
          val updatedContext = head match {
            case op: ArithmeticOp =>
              op.interpret(verbose)
            case op: BitwiseLogicOp =>
              op.interpret(verbose)
            case op: ConstantOp =>
              op.interpret(verbose)
            case op: CryptoOp =>
              op.interpret(verbose)
            case op: FlowControlOp =>
              op.interpret(verbose)
            case op: LocktimeOp =>
              op.interpret(verbose)
            case op: PseudoOp =>
              op.interpret(verbose)
            case op: ReservedOp =>
              op.interpret(verbose)
            case op: SpliceOp =>
              op.interpret(verbose)
            case op: StackOp =>
              op.interpret(verbose)
          }

          for {
            _ <- setState(state.copy(currentScript = tail))
            _ <- updatedContext
            result <- interpret(verbose)
          } yield result

        case Nil =>
          state.stack match {
            case Nil =>
              evaluated(false)
            case head :: tail =>
              evaluated(head.bytes.toBoolean())
/*            case head :: _ if state.p2shScript.isDefined =>
              evaluated(head.bytes.toBoolean())
            case head :: tail =>
              if (head.bytes.toBoolean()) {
                if (state.p2sh() && isP2SHScript(state.scriptPubKey)) {

                  getSerializedScript(state.scriptSig) match {
                    case Some(serializedScript) =>
                      val payToScript = Parser.parse(serializedScript.bytes)

                      println(s"serializedScript: $serializedScript")
                      println(s"payToScript: ${payToScript}")
                      setState(
                        state.copy(
                          currentScript = payToScript,
                          stack = tail,
                          p2shScript = Some(payToScript)
                        )
                      ).flatMap(_ => interpret(verbose))
                    case None =>
                      throw new RuntimeException("xxx")
                  }


                } else {
                  evaluated(!state.requireCleanStack())
                }
              } else {
                evaluated(false)
              }*/

          }
      }
    }
  }

  def getState: InterpreterContext[InterpreterState] = {
    StateT.get[InterpreterErrorHandler, InterpreterState]
  }

  def setState(newState: InterpreterState): InterpreterContext[Unit] = {
    StateT.set[InterpreterErrorHandler, InterpreterState](newState)
  }

  def updateState(updateStateFun: InterpreterState => InterpreterState): InterpreterContext[Unit] = {
    getState.flatMap { oldState =>
      val newState = updateStateFun(oldState)
      setState(newState)
    }
  }

  def evaluated(result: Boolean): InterpreterContext[Option[Boolean]] = {
    StateT.pure(Some(result))
  }

  def continue: Any => InterpreterContext[Option[Boolean]] = {
    _ => StateT.pure(None)
  }

  def abort(error: InterpreterError): InterpreterContext[Option[Boolean]] = StateT.lift {
    val errorWithExplicitType: InterpreterErrorHandler[Option[Boolean]] = Left(error)
    errorWithExplicitType
  }

  private def isP2SHScript(scriptPubkey: Seq[ScriptElement]): Boolean = {
    scriptPubkey.headOption.exists(_ == CryptoOp.OP_HASH160) &&
      scriptPubkey.lastOption.exists(_ == BitwiseLogicOp.OP_EQUAL)
  }

  private def getSerializedScript(scriptSig: Seq[ScriptElement]): Option[ScriptElement] = {
    val fromLastPushOp = scriptSig.reverse.dropWhile { element =>
      element.isInstanceOf[OP_PUSHDATA] ||
        element == OP_PUSHDATA1 ||
        element == OP_PUSHDATA2 ||
        element == OP_PUSHDATA4
    }

    fromLastPushOp.headOption
  }
}
