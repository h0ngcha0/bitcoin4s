package me.hongchao.bitcoin4s.script

import io.github.yzernik.bitcoinscodec.messages.{RegularTx, Tx, TxWitness}
import io.github.yzernik.bitcoinscodec.structures._
import me.hongchao.bitcoin4s.crypto.Hash.Hash256
import me.hongchao.bitcoin4s.script.ConstantOp.OP_0
import scodec.bits.ByteVector
import me.hongchao.bitcoin4s.script.TransactionOps._
import me.hongchao.bitcoin4s.Spec
import cats.implicits._
import me.hongchao.bitcoin4s.script.Interpreter.InterpreterErrorHandler
import me.hongchao.bitcoin4s.script.InterpreterError._

import scala.reflect.ClassTag

trait ScriptTestRunner { self: Spec =>
  sealed trait ExpectedResult extends Product {
    val name = productPrefix
    override def toString: String = name
  }

  object ExpectedResult {
    case object OK extends ExpectedResult
    case object EVAL_FALSE extends ExpectedResult
    case object BAD_OPCODE extends ExpectedResult
    case object UNBALANCED_CONDITIONAL extends ExpectedResult
    case object OP_RETURN extends ExpectedResult
    case object VERIFY extends ExpectedResult
    case object INVALID_ALTSTACK_OPERATION extends ExpectedResult
    case object INVALID_STACK_OPERATION extends ExpectedResult
    case object EQUALVERIFY extends ExpectedResult
    case object DISABLED_OPCODE extends ExpectedResult
    case object UNKNOWN_ERROR extends ExpectedResult
    case object DISCOURAGE_UPGRADABLE_NOPS extends ExpectedResult
    case object PUSH_SIZE extends ExpectedResult
    case object OP_COUNT extends ExpectedResult
    case object STACK_SIZE extends ExpectedResult
    case object SCRIPT_SIZE extends ExpectedResult
    case object PUBKEY_COUNT extends ExpectedResult
    case object SIG_COUNT extends ExpectedResult
    case object SIG_PUSHONLY extends ExpectedResult
    case object MINIMALDATA extends ExpectedResult
    case object PUBKEYTYPE extends ExpectedResult
    case object SIG_DER extends ExpectedResult
    case object WITNESS_PROGRAM_MISMATCH extends ExpectedResult
    case object NULLFAIL extends ExpectedResult
    case object SIG_HIGH_S extends ExpectedResult
    case object SIG_HASHTYPE extends ExpectedResult
    case object SIG_NULLDUMMY extends ExpectedResult
    case object CLEANSTACK extends ExpectedResult
    case object DISCOURAGE_UPGRADABLE_WITNESS_PROGRAM extends ExpectedResult
    case object WITNESS_PROGRAM_WRONG_LENGTH extends ExpectedResult
    case object WITNESS_PROGRAM_WITNESS_EMPTY extends ExpectedResult
    case object WITNESS_MALLEATED extends ExpectedResult
    case object WITNESS_MALLEATED_P2SH extends ExpectedResult
    case object WITNESS_UNEXPECTED extends ExpectedResult
    case object WITNESS_PUBKEYTYPE extends ExpectedResult
    case object NEGATIVE_LOCKTIME extends ExpectedResult
    case object UNSATISFIED_LOCKTIME extends ExpectedResult
    case object MINIMALIF extends ExpectedResult // https://lists.linuxfoundation.org/pipermail/bitcoin-dev/2016-August/013014.html

    val all = Seq(
      OK, EVAL_FALSE, BAD_OPCODE, UNBALANCED_CONDITIONAL, OP_RETURN, VERIFY,
      INVALID_ALTSTACK_OPERATION, INVALID_STACK_OPERATION, EQUALVERIFY,
      DISABLED_OPCODE, UNKNOWN_ERROR, DISCOURAGE_UPGRADABLE_NOPS, PUSH_SIZE,
      OP_COUNT, STACK_SIZE, SCRIPT_SIZE, PUBKEY_COUNT, SIG_COUNT, SIG_PUSHONLY,
      MINIMALDATA, PUBKEYTYPE, SIG_DER, WITNESS_PROGRAM_MISMATCH, NULLFAIL,
      SIG_HIGH_S, SIG_HASHTYPE, SIG_NULLDUMMY, CLEANSTACK,
      DISCOURAGE_UPGRADABLE_WITNESS_PROGRAM, WITNESS_PROGRAM_WRONG_LENGTH,
      WITNESS_PROGRAM_WITNESS_EMPTY, WITNESS_MALLEATED, WITNESS_MALLEATED_P2SH,
      WITNESS_UNEXPECTED, WITNESS_PUBKEYTYPE, NEGATIVE_LOCKTIME, UNSATISFIED_LOCKTIME,
      MINIMALIF
    )

    def fromString(str: String) = all.find(_.name == str)
  }

  case class TestCase(
    scriptSig: Seq[ScriptElement],
    scriptPubKey: Seq[ScriptElement],
    scriptFlags: Seq[ScriptFlag],
    expectedResult: ExpectedResult,
    comments: String,
    witness: Option[(Seq[ScriptConstant], Long)],
    raw: String
  )


  def run(test: TestCase, testNumber: Int) = {
    println(s"\n\nTest $testNumber: $test\n\n")

    val amount = test.witness.map(_._2)
    val creditingTx = creditingTransaction(test.scriptPubKey.flatMap(_.bytes), amount)
    val spendingTx = spendingTransaction(creditingTx, test.scriptSig.flatMap(_.bytes), test.witness.map(_._1))

    val initialState = InterpreterState(
      scriptPubKey = test.scriptPubKey,
      scriptSig = test.scriptSig,
      scriptWitnessStack = test.witness.map(_._1),
      flags = test.scriptFlags,
      transaction = spendingTx,
      inputIndex = 0,
      amount = amount.getOrElse(0)
    )

    withClue(test.comments) {
      val result = Interpreter.interpret(verbose = false).run(initialState)
      implicit val expectedResult = test.expectedResult

      expectedResult match {
        case ExpectedResult.OK =>
          result match {
            case Right((finalState@_, result)) =>
              result shouldEqual Some(true)
            case Left(error) =>
              fail(error.toString, error)
          }

        case ExpectedResult.EVAL_FALSE =>
          result match {
            case Right((finalState@_, result)) =>
              result shouldEqual Some(false)
            case Left(error) =>
              fail(error)
          }

        case ExpectedResult.BAD_OPCODE =>
          checkError[BadOpCode](result)

        case ExpectedResult.CLEANSTACK =>
          checkError[RequireCleanStack](result)

        case ExpectedResult.DISABLED_OPCODE =>
          checkError[OpcodeDisabled](result)

        case ExpectedResult.DISCOURAGE_UPGRADABLE_NOPS =>
          checkError[DiscourageUpgradableNops](result)

        case ExpectedResult.EQUALVERIFY =>
          checkError[VerificationFailed](result)

        case ExpectedResult.INVALID_ALTSTACK_OPERATION =>
          checkError[InvalidAltStackOperation](result)

        case ExpectedResult.INVALID_STACK_OPERATION =>
          checkError[InvalidStackOperation](result)

        case ExpectedResult.MINIMALDATA =>
          checkError[NotMinimalEncoding](result)

        case ExpectedResult.UNBALANCED_CONDITIONAL =>
          checkError[UnbalancedConditional](result)

        case ExpectedResult.NEGATIVE_LOCKTIME =>
          checkError[CSVFailed](result)

        case ExpectedResult.OP_COUNT =>
          checkError[ExceedMaxOpCount](result)

        case ExpectedResult.OP_RETURN =>
          checkError[FoundOpReturn](result)

        case ExpectedResult.VERIFY =>
          checkError[VerificationFailed](result)

        case ExpectedResult.PUSH_SIZE =>
          checkError[ExceedMaxPushSize](result)

        case ExpectedResult.STACK_SIZE =>
          checkError[ExceedMaxStackSize](result)

        case ExpectedResult.SCRIPT_SIZE =>
          checkError[ExceedMaxScriptSize](result)

        case ExpectedResult.PUBKEY_COUNT =>
          checkError[WrongPubKeyCount](result)

        case ExpectedResult.SIG_COUNT =>
          checkError[WrongSignaturesCount](result)

        case ExpectedResult.SIG_PUSHONLY =>
          checkError[ScriptSigPushOnly](result)

        case ExpectedResult.PUBKEYTYPE =>
          checkError[PublicKeyWrongEncoding](result)

        case ExpectedResult.SIG_DER =>
          checkError[SignatureWrongEncoding](result)

        case ExpectedResult.NULLFAIL =>
          checkError[SignatureVerificationNullFail](result)

        case ExpectedResult.SIG_NULLDUMMY =>
          checkError[MultiSigNullDummy](result)

        case ExpectedResult.WITNESS_PROGRAM_MISMATCH =>
          checkError[WitnessProgramMismatch](result)

        case ExpectedResult.WITNESS_PROGRAM_WRONG_LENGTH =>
          checkError[WitnessProgramWrongLength](result)

        case ExpectedResult.WITNESS_PROGRAM_WITNESS_EMPTY =>
          checkError[WitnessProgramWitnessEmpty](result)

        case ExpectedResult.SIG_HIGH_S =>
          checkError[SignatureHighS](result)

        case ExpectedResult.SIG_HASHTYPE =>
          checkError[InvalidSigHashType](result)

        case ExpectedResult.DISCOURAGE_UPGRADABLE_WITNESS_PROGRAM =>
          checkError[DiscourageUpgradableWitnessProgram](result)

        case _ =>
          throw new NotImplementedError()
      }
    }
  }

  private def checkError[T: ClassTag](
    result: InterpreterErrorHandler[(InterpreterState, Option[Boolean])]
  )(implicit expectedResult: ExpectedResult) = {
    result match {
      case Right((finalState, result)) =>
        fail(s"Expect ${expectedResult.name} but receive $result")
      case Left(error) =>
        error shouldBe a [T]
    }
  }

  def creditingTransaction(scriptPubKey: Seq[Byte], maybeAmount: Option[Long]) = {
    val emptyTxId = Array.fill[Byte](32)(0)
    val emptyOutpoint = OutPoint(Hash.NULL, -1)
    val maxSequence = 0xffffffff
    val txIn = TxIn(
      previous_output = emptyOutpoint,
      sig_script = ByteVector(Seq(OP_0, OP_0).flatMap(_.bytes)),
      sequence = maxSequence
    )

    maybeAmount match {
      case Some(amount) =>
        val txOut = TxOutWitness(value = amount, pk_script = ByteVector(scriptPubKey))
        TxWitness(
          version = 1,
          tx_in = txIn :: Nil,
          tx_out = txOut :: Nil,
          lock_time = 0
        )
      case None =>
        val txOut = RegularTxOut(value = 0, pk_script = ByteVector(scriptPubKey))
        RegularTx(
          version = 1,
          tx_in = txIn :: Nil,
          tx_out = txOut :: Nil,
          lock_time = 0
        )
    }
  }

  def spendingTransaction(creditingTransaction: Tx, scriptSig: Seq[Byte], maybeWitnessScript: Option[Seq[ScriptConstant]]) = {
    val maxSequence = 0xffffffff

    import scodec.bits._

    val prevId = Hash(ByteVector(Hash256(creditingTransaction.serialized().toArray)).reverse)
    val txIn = TxIn(
      previous_output = OutPoint(prevId, 0),
      sig_script = ByteVector(scriptSig),
      sequence = maxSequence
    )

    val amount = creditingTransaction.tx_out(0).value

    maybeWitnessScript match {
      case Some(witnessScript) =>
        val txOut = TxOutWitness(value = amount, pk_script = ByteVector.empty)
        TxWitness(
          version = 1,
          tx_in = txIn :: Nil,
          tx_out = txOut :: Nil,
          lock_time = 0
        )
      case None =>
        val txOut = RegularTxOut(value = amount, pk_script = ByteVector.empty)
        RegularTx(
          version = 1,
          tx_in = txIn :: Nil,
          tx_out = txOut :: Nil,
          lock_time = 0
        )
    }

  }
}
