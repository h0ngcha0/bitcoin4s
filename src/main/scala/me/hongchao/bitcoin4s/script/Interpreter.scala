package me.hongchao.bitcoin4s.script

import com.typesafe.scalalogging.StrictLogging
import io.github.yzernik.bitcoinscodec.messages.Tx
import io.github.yzernik.bitcoinscodec.structures.TxIn
import me.hongchao.bitcoin4s.script.OpCodes.OP_UNKNOWN
import me.hongchao.bitcoin4s.script.ConstantOp._
import me.hongchao.bitcoin4s.script.CryptoOp.{OP_CHECKSIG, OP_HASH160}
import me.hongchao.bitcoin4s.script.InterpreterError._
import me.hongchao.bitcoin4s.script.Interpreter._
import me.hongchao.bitcoin4s.Utils._
import ScriptExecutionStage._
import simulacrum._
import cats.FlatMap
import cats.data._
import cats.implicits._
import me.hongchao.bitcoin4s.crypto.Hash
import me.hongchao.bitcoin4s.script.BitwiseLogicOp.OP_EQUALVERIFY
import me.hongchao.bitcoin4s.script.StackOp.OP_DUP

sealed trait ScriptExecutionStage
object ScriptExecutionStage {
  case object ExecutingScriptSig extends ScriptExecutionStage
  case object ExecutingScriptPubKey extends ScriptExecutionStage
  case object ExecutingScriptP2SH extends ScriptExecutionStage
  case object ExecutingScriptWitness extends ScriptExecutionStage
}

object InterpreterState {
  def apply(
    scriptPubKey: Seq[ScriptElement],
    scriptSig: Seq[ScriptElement],
    scriptWitnessStack: Option[Seq[ScriptElement]],
    flags: Seq[ScriptFlag],
    transaction: Tx,
    inputIndex: Int,
    amount: Long
  ): InterpreterState = {
    InterpreterState(
      scriptPubKey = scriptPubKey,
      scriptSig = scriptSig,
      scriptWitnessStack = scriptWitnessStack,
      currentScript = scriptSig,
      transaction = transaction,
      inputIndex = inputIndex,
      flags = flags,
      sigVersion = SigVersion.SIGVERSION_BASE,
      amount = amount
    )
  }
}

case class InterpreterState(
  scriptPubKey: Seq[ScriptElement],
  scriptSig: Seq[ScriptElement],
  currentScript: Seq[ScriptElement],
  scriptP2sh: Option[Seq[ScriptElement]] = None,
  scriptWitness: Option[Seq[ScriptElement]] = None,
  scriptWitnessStack: Option[Seq[ScriptElement]] = None,
  stack: Seq[ScriptElement] = Seq.empty,
  altStack: Seq[ScriptElement] = Seq.empty,
  flags: Seq[ScriptFlag],
  opCount: Int = 0,
  transaction: Tx,
  inputIndex: Int,
  amount: Long,
  sigVersion: SigVersion,
  scriptExecutionStage: ScriptExecutionStage = ScriptExecutionStage.ExecutingScriptSig
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

  object ScriptFlags {
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

    def pushOnly(): Boolean = {
      flags.contains(ScriptFlag.SCRIPT_VERIFY_SIGPUSHONLY)
    }

    def strictEncoding(): Boolean = {
      flags.contains(ScriptFlag.SCRIPT_VERIFY_STRICTENC)
    }

    def derSig(): Boolean = {
      flags.contains(ScriptFlag.SCRIPT_VERIFY_DERSIG)
    }

    def nullfail(): Boolean = {
      flags.contains(ScriptFlag.SCRIPT_VERIFY_NULLFAIL)
    }

    def nulldummy(): Boolean = {
      flags.contains(ScriptFlag.SCRIPT_VERIFY_NULLDUMMY)
    }

    def witness(): Boolean = {
      flags.contains(ScriptFlag.SCRIPT_VERIFY_WITNESS)
    }
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

  val MAX_OPCODES = 201
  val MAX_PUSH_SIZE = 520
  val MAX_STACK_SIZE = 1000
  val MAX_SCRIPT_SIZE = 10000  // bytes

  def tailRecM[A, B] = FlatMap[InterpreterContext].tailRecM[A, B] _

  import me.hongchao.bitcoin4s.script.Interpretable.ops._

  def interpret(verbose: Boolean = false): InterpreterContext[Option[Boolean]] = {
    for {
      _ <- checkInvalidOpCode()
      _ <- checkDisabledOpCode()
      _ <- checkMaxPushSize()
      _ <- checkMaxScriptSize()
      _ <- checkIsPushOnly()

      result <- interpretScript(verbose)
    } yield result
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

  private def checkOpCodeCount(): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      (state.opCount > MAX_OPCODES)
        .option(abort(ExceedMaxOpCount(OP_UNKNOWN, state)))
        .getOrElse(StateT.pure(None))
    }
  }

  private def checkInvalidOpCode(): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      state.currentScript.find(OpCodes.invalid.contains) match {
        case Some(opCode: ScriptOpCode) =>
          abort(BadOpCode(opCode, state, "Opcode not allowed"))
        case _ =>
          StateT.pure(None)
      }
    }
  }

  private def checkDisabledOpCode(): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      state.currentScript.find(OpCodes.disabled.contains) match {
        case Some(opCode: ScriptOpCode) =>
          abort(OpcodeDisabled(opCode, state))
        case _ =>
          StateT.pure(None)
      }
    }
  }

  private def checkMaxPushSize(): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      state.currentScript.filter(_.isInstanceOf[ScriptConstant]).filter(_.bytes.length > MAX_PUSH_SIZE) match {
        case head :: tail =>
          abort(ExceedMaxPushSize(OP_UNKNOWN, state))
        case Nil =>
          StateT.pure(None)
      }
    }
  }

  private def checkMaxStackSize(): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      val totalStackSize = (state.stack ++ state.altStack).length
      if (totalStackSize > MAX_STACK_SIZE) {
        abort(ExceedMaxStackSize(OP_UNKNOWN, state))
      } else {
        StateT.pure(None)
      }
    }
  }

  private def checkMaxScriptSize(): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      val numberOfPushData1 = state.currentScript.filter(_ == OP_PUSHDATA1).length
      val numberOfPushData2 = state.currentScript.filter(_ == OP_PUSHDATA2).length
      val numberOfPushData4 = state.currentScript.filter(_ == OP_PUSHDATA4).length
      val skippedDataLengthBytes = numberOfPushData1 + numberOfPushData2 * 2 + numberOfPushData4 * 4

      val totalBytes = state.currentScript.map(_.bytes.length).sum + skippedDataLengthBytes

      if (totalBytes > MAX_SCRIPT_SIZE) {
        abort(ExceedMaxScriptSize(OP_UNKNOWN, state))
      } else {
        StateT.pure(None)
      }
    }
  }

  private def checkIsPushOnly(): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      val pushOnlyEnabled = state.ScriptFlags.pushOnly()
      val shouldExecuteP2sh = isP2SHScript(state.scriptPubKey) && state.ScriptFlags.p2sh()
      val shouldCheckPushOnly = pushOnlyEnabled || shouldExecuteP2sh

      if (shouldCheckPushOnly && !isPushOnly(state.scriptSig)) {
        abort(ScriptSigPushOnly(OP_UNKNOWN, state))
      } else {
        StateT.pure(None)
      }
    }
  }

  private def interpretScript(verbose: Boolean): InterpreterContext[Option[Boolean]] = {
    tailRecM(None: Option[Boolean]) {
      case Some(value) =>
        StateT.pure(Right(Some(value)))
      case None =>
        for {
          state <- getState
          _ <- checkOpCodeCount()
          _ <- checkMaxStackSize()
          result <- interpretOneOp(state, verbose)
        } yield result
    }
  }

  private def interpretOneOp(state: InterpreterState, verbose: Boolean): InterpreterContext[Either[Option[Boolean], Option[Boolean]]] = {
    state.currentScript match {
      case opCode :: tail =>
        val updatedContext = opCode match {
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
          result <- updatedContext
        } yield {
          result match {
            case Some(value) =>
              Right(Some(value))
            case None =>
              Left(None)
          }
        }

      case Nil =>
        state.scriptExecutionStage match {
          case ExecutingScriptSig =>
            for {
              _ <- setState(state.copy(
                currentScript = state.scriptPubKey,
                altStack = Seq.empty,
                opCount = 0,
                scriptExecutionStage = ExecutingScriptPubKey
              ))
              _ <- checkInvalidOpCode()
              _ <- checkDisabledOpCode()
              _ <- checkMaxPushSize()
              _ <- checkMaxScriptSize()
            } yield {
              Left(None)
            }

          case ExecutingScriptPubKey =>
            state.stack match {
              case Nil =>
                tailRecMEvaluated(false)
              case head :: Nil =>
                tailRecMEvaluated(head.bytes.toBoolean())
              case head :: tail =>
                if (state.ScriptFlags.witness()) {

                  tryRebuildScriptPubkeyAndStackFromWitness(state.scriptPubKey, state.scriptWitnessStack) match {
                    case Some((rebuiltScriptPubkey, rebuiltStack)) =>
                      for {
                        _ <- setState(state.copy(
                          currentScript = rebuiltScriptPubkey,
                          stack = rebuiltStack,
                          altStack = Seq.empty,
                          opCount = 0,
                          scriptWitness = Some(rebuiltScriptPubkey),
                          sigVersion = SigVersion.SIGVERSION_WITNESS_V0,
                          scriptExecutionStage = ExecutingScriptWitness
                        ))
                        _ <- checkInvalidOpCode()
                        _ <- checkDisabledOpCode()
                        _ <- checkMaxPushSize()
                        _ <- checkMaxScriptSize()
                      } yield {
                        Left(None)
                      }

                    case None =>
                      tailRecMAbort(GeneralError(OP_UNKNOWN, state))
                  }

                } else if (state.ScriptFlags.p2sh() && isP2SHScript(state.scriptPubKey)) {
                  getSerializedScript(state.scriptSig) match {
                    case Some(serializedScript) =>
                      val payToScript = Parser.parse(serializedScript.bytes)

                      for {
                        _ <- setState(state.copy(
                          currentScript = payToScript,
                          stack = tail,
                          altStack = Seq.empty,
                          opCount = 0,
                          scriptP2sh = Some(payToScript),
                          scriptExecutionStage = ExecutingScriptP2SH
                        ))
                        _ <- checkInvalidOpCode()
                        _ <- checkDisabledOpCode()
                        _ <- checkMaxPushSize()
                        _ <- checkMaxScriptSize()
                      } yield {
                        Left(None)
                      }
                    case None =>
                      tailRecMAbort(NoSerializedScriptFound(OP_HASH160, state))
                  }
                } else {
                  if (state.ScriptFlags.requireCleanStack()) {
                    tailRecMAbort(RequireCleanStack(OP_UNKNOWN, state))
                  } else {
                    tailRecMEvaluated(head.bytes.toBoolean())
                  }
                }
            }

          case ExecutingScriptP2SH | ExecutingScriptWitness =>
            state.stack match {
              case head :: tail =>
                if (state.ScriptFlags.requireCleanStack() && tail.nonEmpty) {
                  tailRecMAbort(RequireCleanStack(OP_UNKNOWN, state))
                } else {
                  StateT.pure(Right(Some(head.bytes.toBoolean())))
                }
            }
        }
    }
  }

  private def isP2SHScript(scriptPubkey: Seq[ScriptElement]): Boolean = {
    scriptPubkey.headOption.exists(_ == CryptoOp.OP_HASH160) &&
      scriptPubkey.lastOption.exists(_ == BitwiseLogicOp.OP_EQUAL)
  }

  private def getWitnessScript(scriptPubkey: Seq[ScriptElement]): Option[(ConstantOp, ScriptConstant)] = {
    import ConstantOp._

    val possibleVersionNumbers = Seq(
      OP_0, OP_FALSE, OP_1, OP_TRUE, OP_2, OP_3, OP_4, OP_5, OP_6, OP_7,
      OP_8, OP_9, OP_10, OP_11, OP_12, OP_13, OP_14, OP_15, OP_16
    )

    scriptPubkey match {
      case (version: ConstantOp) :: (_: OP_PUSHDATA) :: (scriptConstant: ScriptConstant) :: Nil =>
        val scriptLength = scriptConstant.bytes.length
        val isWitnessScript = possibleVersionNumbers.contains(version) && (
          scriptLength >= 2 && scriptLength <= 40
        )

        isWitnessScript.option((version, scriptConstant))

      case _ =>
        None
    }
  }

  private def tryRebuildScriptPubkeyAndStackFromWitness(
    scriptPubkey: Seq[ScriptElement],
    maybeWitnessStack: Option[Seq[ScriptElement]]
  ): Option[(Seq[ScriptElement], Seq[ScriptElement])] = {
    def rebuildScriptPubkeyAndStackFromWitness(witnessHash: ScriptConstant, witnessStack: Seq[ScriptElement]) = {
      if (witnessHash.bytes.length == 20) {
        // P2WPKH
        val scriptPubKey = OP_DUP :: OP_HASH160 :: OP_PUSHDATA(20) :: witnessHash :: OP_EQUALVERIFY :: OP_CHECKSIG :: Nil
        Some((scriptPubKey, witnessStack))
      } else if (witnessHash.bytes.length == 32) {
        // P2WPSH
        val head = witnessStack.head

        for {
          head <- witnessStack.headOption
          scriptPubKey <- (Hash.Sha256(head.bytes.toArray).toSeq == witnessHash.bytes).option(Parser.parse(head.bytes))
        } yield {
          (scriptPubKey, removePushOps(witnessStack.tail))
        }
      } else {
        None
      }
    }

    for {
      (version, witnessHash) <- getWitnessScript(scriptPubkey)
      witnessStack <- maybeWitnessStack
      result <- rebuildScriptPubkeyAndStackFromWitness(witnessHash, witnessStack)
    } yield result
  }

  private def isPushOnly(script: Seq[ScriptElement]): Boolean = {
    script.filterNot(_.isInstanceOf[ScriptConstant]).forall(ConstantOp.all.contains)
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

  private def removePushOps(script: Seq[ScriptElement]): Seq[ScriptElement] = {
    script.filterNot { element =>
      element.isInstanceOf[OP_PUSHDATA] ||
        element == OP_PUSHDATA1 ||
        element == OP_PUSHDATA2 ||
        element == OP_PUSHDATA4
    }
  }

  def tailRecMAbort(error: InterpreterError): InterpreterContext[Either[Option[Boolean],Option[Boolean]]] = StateT.lift {
    val errorWithExplicitType: InterpreterErrorHandler[Either[Option[Boolean],Option[Boolean]]] = Left(error)
    errorWithExplicitType
  }

  def tailRecMEvaluated(value: Boolean): InterpreterContext[Either[Option[Boolean], Option[Boolean]]] = {
    StateT.pure(Right(Some(value)))
  }
}
