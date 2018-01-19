package me.hongchao.bitcoin4s.script

import me.hongchao.bitcoin4s.script.Interpreter._
import me.hongchao.bitcoin4s.script.InterpreterError._
import cats.implicits._
import io.github.yzernik.bitcoinscodec.messages.Tx
import me.hongchao.bitcoin4s.script.RichTransaction._

sealed trait LocktimeOp extends ScriptOpCode

object LocktimeOp {
  case object OP_CHECKLOCKTIMEVERIFY extends LocktimeOp { val value = 177 }
  case object OP_CHECKSEQUENCEVERIFY extends LocktimeOp { val value = 178 }
  case object OP_NOP2 extends LocktimeOp { val value = 177 }
  case object OP_NOP3 extends LocktimeOp { val value = 178 }

  val all = Seq(OP_CHECKLOCKTIMEVERIFY, OP_CHECKSEQUENCEVERIFY, OP_NOP2, OP_NOP3)

  implicit val interpreter = new Interpretable[LocktimeOp] {
    def interpret(opCode: LocktimeOp): InterpreterContext[Option[Boolean]] = {
      opCode match {
        case OP_CHECKLOCKTIMEVERIFY | OP_NOP2 =>
          getState.flatMap { state =>
            if (state.ScriptFlags.cltvEnabled) {
              state.stack match {
                case head :: _ =>
                  val lockTime = ScriptNum(head.bytes, false, 5).value
                  if (lockTime < 0 || lockTime <= state.transaction.lock_time) {
                    abort(CLTVFailed(opCode, state))
                  } else {
                    setState(state.replaceStackTopElement(ScriptNum(1))).flatMap(continue)
                  }

                case Nil =>
                  abort(NotEnoughElementsInStack(opCode, state))
              }
            } else if (state.ScriptFlags.disCourageUpgradableNop) {
              abort(DiscourageUpgradableNops(opCode, state))
            } else {
              continue(opCode)
            }
          }

        case OP_CHECKSEQUENCEVERIFY | OP_NOP3 =>
          getState.flatMap { state =>
            if (state.ScriptFlags.csvEnabled) {
              state.stack match {
                case head :: _ =>
                  val sequence = ScriptNum(head.bytes, false, 5).value
                  val txIn = state.transaction.tx_in(state.inputIndex)

                  // NOTE, Mimic the logic here:
                  // https://github.com/bitcoin/bitcoin/blob/5961b23898ee7c0af2626c46d5d70e80136578d3/src/script/interpreter.cpp#L413
                  // To provide for future soft-fork extensibility, if the
                  // operand has the disabled lock-time flag set,
                  // CHECKSEQUENCEVERIFY behaves as a NOP.
                  val csvDisabled = (sequence & txIn.SEQUENCE_LOCKTIME_DISABLE_FLAG) != 0

                  if (sequence < 0) {
                    abort(CSVFailed(opCode, state))
                  } else if (csvDisabled) {
                    setState(state.copy(opCount = state.opCount + 1)).flatMap(continue)
                  } else if (!checkSequence(sequence, state.transaction, state.inputIndex)) {
                    abort(CSVFailed(opCode, state))
                  } else {
                    setState(state.replaceStackTopElement(ScriptNum(1))).flatMap(continue)
                  }

                case Nil =>
                  abort(InvalidStackOperation(opCode, state))
              }
            } else if (state.ScriptFlags.disCourageUpgradableNop) {
              abort(DiscourageUpgradableNops(opCode, state))
            } else {
              continue(opCode)
            }
          }
      }
    }

    // NOTE: Mimic the logic here:
    // https://github.com/bitcoin/bitcoin/blob/5961b23898ee7c0af2626c46d5d70e80136578d3/src/script/interpreter.cpp#L1310
    private def checkSequence(nSequence: Long, transaction: Tx, inputIndex: Int): Boolean = {
      val txIn = transaction.tx_in(inputIndex)
      val txInSequence = txIn.sequence

      // Fail if the transaction's version number is not set high
      // enough to trigger BIP 68 rules.
      val transactionVersionNotHighEnough = transaction.version < 2

      // Sequence numbers with their most significant bit set are not
      // consensus constrained. Testing that the transaction's sequence
      // number do not have this bit set prevents using this property
      // to get around a CHECKSEQUENCEVERIFY check.
      val csvEnabled = (txInSequence & txIn.SEQUENCE_LOCKTIME_DISABLE_FLAG) == 0

      // Mask off any bits that do not have consensus-enforced meaning
      // before doing the integer comparisons
      val nLockTimeMask = txIn.SEQUENCE_LOCKTIME_TYPE_FLAG | txIn.SEQUENCE_LOCKTIME_MASK
      val txToSequenceMasked = txInSequence & nLockTimeMask
      val nSequenceMasked = nSequence & nLockTimeMask

      // There are two kinds of nSequence: lock-by-blockheight
      // and lock-by-blocktime, distinguished by whether
      // nSequenceMasked < CTxIn::SEQUENCE_LOCKTIME_TYPE_FLAG.
      //
      // We want to compare apples to apples, so fail the script
      // unless the type of nSequenceMasked being tested is the same as
      // the nSequenceMasked in the transaction.
      val bothLockByBlockHeight = (txToSequenceMasked <  txIn.SEQUENCE_LOCKTIME_TYPE_FLAG && nSequenceMasked <  txIn.SEQUENCE_LOCKTIME_TYPE_FLAG)
      val bothLockByBlockTime = (txToSequenceMasked >= txIn.SEQUENCE_LOCKTIME_TYPE_FLAG && nSequenceMasked >= txIn.SEQUENCE_LOCKTIME_TYPE_FLAG)
      val sameKindOfNSequence = bothLockByBlockHeight || bothLockByBlockTime

      transactionVersionNotHighEnough &&
        csvEnabled &&
        sameKindOfNSequence &&
        txToSequenceMasked > nSequenceMasked
    }
  }
}
