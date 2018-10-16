package it.softfork.bitcoin4s.script

import it.softfork.bitcoin4s.transaction._
import it.softfork.bitcoin4s.script.CryptoOp.OP_CODESEPARATOR
import scodec.bits.ByteVector
import it.softfork.bitcoin4s.Utils._
import it.softfork.bitcoin4s.crypto.Hash
import it.softfork.bitcoin4s.transaction.structure.OutPoint

object RichTransaction {
  val transactionVersion = 1

  implicit class RichTx(tx: Tx) {
    def serialize(): ByteVector = {
      Tx.codec(transactionVersion).encode(tx).toEither match {
        case Left(error) =>
          throw new RuntimeException(error.messageWithContext)
        case Right(v) =>
          v.toByteVector
      }
    }

    def signingHashPreSegwit(pubKeyScript: Seq[ScriptElement], inputIndex: Int, sigHashType: SignatureHashType): Seq[Byte] = {
      val updatedTx0 = tx
        .removeSigScript()
        .updateTxInWithPubKeyScript(pubKeyScript, inputIndex)

      val updatedTx1 =
        if (sigHashType.SIGHASH_NONE()) {
          updatedTx0
            .resetSequence(inputIndex)
            .setAllTxOutputToEmpty()
        } else if (sigHashType.SIGHASH_SINGLE()) {
          updatedTx0
            .resetSequence(inputIndex)
            .setAllTxOutputExceptOne(inputIndex)
        } else {
          updatedTx0
        }

      val updatedTx = sigHashType.SIGHASH_ANYONECANPAY()
        .option(
          updatedTx1.copy(
            tx_in = updatedTx1.tx_in
              .zipWithIndex
              .filter(_._2 == inputIndex)
              .map(_._1)
          )
        )
        .getOrElse(updatedTx1)

      val serialisedTx = Tx.codec(transactionVersion).compact.encode(updatedTx).toEither match {
        case Left(error) =>
          throw new RuntimeException(error.messageWithContext)
        case Right(v) =>
          v.toByteArray
      }

      val preImage = serialisedTx ++ uint32ToBytes(sigHashType.value)

      println("transaction preImage:")
      println(preImage.toSeq.toHex)

      Hash.Hash256(preImage)
    }


    // https://github.com/bitcoin/bips/blob/master/bip-0143.mediawiki
    // https://github.com/bitcoin/bitcoin/blob/f8528134fc188abc5c7175a19680206964a8fade/src/script/interpreter.cpp#L1113
    def signingHashSegwit(pubKeyScript: Seq[ScriptElement], inputIndex: Int, amount: Long, sigHashType: SignatureHashType): Seq[Byte] = {
      val prevOutsHash: Array[Byte] = if(!sigHashType.SIGHASH_ANYONECANPAY()) {
        val prevOut = tx.tx_in.toArray.flatMap { txIn =>
          OutPoint.codec.encode(txIn.previous_output).toBytes
        }
        Hash.Hash256(prevOut)
      } else {
        Hash.zeros
      }

      val txIn = tx.tx_in(inputIndex)

      val sequencesHash = if (!sigHashType.SIGHASH_ANYONECANPAY() && !sigHashType.SIGHASH_NONE() && !sigHashType.SIGHASH_SINGLE()) {
        val sequenceBytes = tx.tx_in.toArray.flatMap { txIn => uint32ToBytes(txIn.sequence) }
        Hash.Hash256(sequenceBytes)
      } else {
        Hash.zeros
      }

      val prevOutBytes = OutPoint.codec.encode(txIn.previous_output).toBytes

      val encodedScriptBytes = {
        val scriptBytes = pubKeyScript.flatMap(_.bytes).toArray
        TxIn.scriptCodec.encode(ByteVector(scriptBytes)).toBytes
      }

      val amountBytes = uInt64ToBytes(amount)

      val sequenceBytes = uint32ToBytes(txIn.sequence)

      val outputHash = if (!sigHashType.SIGHASH_SINGLE() && !sigHashType.SIGHASH_NONE()) {
        val outputBytes: Array[Byte] = tx.tx_out.toArray.flatMap(TxOut.codec.encode(_).toBytes)
        Hash.Hash256(outputBytes)
      } else if (sigHashType.SIGHASH_SINGLE() && inputIndex < tx.tx_out.length) {
        val txOut = tx.tx_out(inputIndex)
        val outputBytes = TxOut.codec.encode(txOut).toBytes
        Hash.Hash256(outputBytes)
      } else {
        Hash.zeros
      }

      val versionBytes = uint32ToBytes(tx.version)

      val locktimeBytes = uint32ToBytes(tx.lock_time)
      val sigHashTypeBytes = uint32ToBytes(sigHashType.value)

      val preImage: Seq[Byte] =
        versionBytes ++
          prevOutsHash ++
          sequencesHash ++
          prevOutBytes ++
          encodedScriptBytes ++
          amountBytes ++
          sequenceBytes ++
          outputHash ++
          locktimeBytes ++
          sigHashTypeBytes

      Hash.Hash256(preImage.toArray)
    }

    def removeSigScript(): Tx = {
      val txIns = tx.tx_in.map { txIn =>
        txIn.copy(sig_script = ByteVector.empty)
      }

      tx.copy(tx_in = txIns)
    }

    // All other inputs aside from the current input in txCopy have their nSequence index set to zero
    def resetSequence(inputIndex: Int): Tx = {
      val newTxIn = tx.tx_in.zipWithIndex.map {
        case (txIn, index) =>
          (index == inputIndex)
            .option(txIn)
            .getOrElse(txIn.copy(sequence = 0))
      }
      tx.copy(tx_in = newTxIn)
    }

    def updateTxInWithPubKeyScript(pubKeyScript: Seq[ScriptElement], inputIndex: Int): Tx = {
      val updatedPubKeyScript = pubKeyScript.filter(_ != OP_CODESEPARATOR)
      val updatedScriptTxIns = tx.tx_in.zipWithIndex.map {
        case (txIn, index) =>
          (index == inputIndex)
            .option(txIn.copy(sig_script = ByteVector(updatedPubKeyScript.flatMap(_.bytes))))
            .getOrElse(txIn)
      }
      tx.copy(tx_in = updatedScriptTxIns)
    }

    def setAllTxOutputToEmpty(): Tx = {
      tx.copy(tx_out = List.empty)
    }

    def setAllTxOutputExceptOne(inputIndex: Int): Tx = {
      val updatedTxOut = tx.tx_out.take(inputIndex).zipWithIndex.map {
        case (txIn, index) =>
          (index == inputIndex)
            .option(txIn)
            .getOrElse(txIn.copy(value = -1, pk_script = ByteVector.empty))
      }

      tx.copy(tx_out = updatedTxOut)
    }
  }

  implicit class RichTxIn(txIn: TxIn) {
    // NOTE: a copy of the following variables in bitcoin core.
    // https://github.com/bitcoin/bitcoin/blob/5961b23898ee7c0af2626c46d5d70e80136578d3/src/primitives/transaction.h#L69

    /* Setting nSequence to this value for every input in a transaction disables nLockTime. */
    val SEQUENCE_FINAL = 0xffffffffL

    /* Below flags apply in the context of BIP 68*/
    /* If this flag set, CTxIn::nSequence is NOT interpreted as a relative lock-time. */
    val SEQUENCE_LOCKTIME_DISABLE_FLAG = (1L << 31)

    /* If CTxIn::nSequence encodes a relative lock-time and this flag
     * is set, the relative lock-time has units of 512 seconds,
     * otherwise it specifies blocks with a granularity of 1. */
    val SEQUENCE_LOCKTIME_TYPE_FLAG = (1L << 22)

    /* If CTxIn::nSequence encodes a relative lock-time, this mask is
     * applied to extract that lock-time from the sequence field. */
    val SEQUENCE_LOCKTIME_MASK = 0x0000ffffL

    /* In order to use the same number of bits to encode roughly the
     * same wall-clock duration, and because blocks are naturally
     * limited to occur every 600s on average, the minimum granularity
     * for time-based relative lock-time is fixed at 512 seconds.
     * Converting from CTxIn::nSequence to seconds is performed by
     * multiplying by 512 = 2^9, or equivalently shifting up by
     * 9 bits. */
    val SEQUENCE_LOCKTIME_GRANULARITY = 9
  }
}

