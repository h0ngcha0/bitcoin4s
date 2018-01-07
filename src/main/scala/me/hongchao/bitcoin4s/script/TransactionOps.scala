package me.hongchao.bitcoin4s.script

import io.github.yzernik.bitcoinscodec.messages.{RegularTx, Tx, TxWitness}
import io.github.yzernik.bitcoinscodec.structures.{OutPoint, TxIn, TxOut, TxOutWitness}
import me.hongchao.bitcoin4s.script.CryptoOp.OP_CODESEPARATOR
import scodec.bits.{BitVector, ByteVector}
import me.hongchao.bitcoin4s.Utils._
import me.hongchao.bitcoin4s.crypto.Hash
import me.hongchao.bitcoin4s.script.SigVersion.{SIGVERSION_BASE, SIGVERSION_WITNESS_V0}
import scodec.Attempt

object TransactionOps {
  val transactionVersion = 1

  implicit class RichRegularTx(tx: RegularTx) {
    def transactionId(): ByteVector = {
      // For witness stuff, whats the transaction id?
      RegularTx.codec(transactionVersion).encode(tx).toEither match {
        case Left(error) =>
          throw new RuntimeException(error.messageWithContext)
        case Right(v) =>
          v.toByteVector
      }
    }

    def signingHash(pubKeyScript: Seq[ScriptElement], inputIndex: Int, sigHashType: SignatureHashType): Seq[Byte] = {
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

      val serialisedTx = RegularTx.codec(transactionVersion).compact.encode(updatedTx).toEither match {
        case Left(error) =>
          throw new RuntimeException(error.messageWithContext)
        case Right(v) =>
          v.toByteArray
      }

      Hash.Hash256(serialisedTx ++ uint32ToBytes(sigHashType.value))
    }

    def removeSigScript(): RegularTx = {
      val txIns = tx.tx_in.map { txIn =>
        txIn.copy(sig_script = ByteVector.empty)
      }

      tx.copy(tx_in = txIns)
    }

    // All other inputs aside from the current input in txCopy have their nSequence index set to zero
    def resetSequence(inputIndex: Int): RegularTx = {
      val newTxIn = tx.tx_in.zipWithIndex.map {
        case (txIn, index) =>
          (index == inputIndex)
            .option(txIn)
            .getOrElse(txIn.copy(sequence = 0))
      }
      tx.copy(tx_in = newTxIn)
    }

    def updateTxInWithPubKeyScript(pubKeyScript: Seq[ScriptElement], inputIndex: Int): RegularTx = {
      val updatedPubKeyScript = pubKeyScript.filter(_ != OP_CODESEPARATOR)
      val updatedScriptTxIns = tx.tx_in.zipWithIndex.map {
        case (txIn, index) =>
          (index == inputIndex)
            .option(txIn.copy(sig_script = ByteVector(updatedPubKeyScript.flatMap(_.bytes))))
            .getOrElse(txIn)
      }
      tx.copy(tx_in = updatedScriptTxIns)
    }

    def setAllTxOutputToEmpty(): RegularTx = {
      tx.copy(tx_out = List.empty)
    }

    def setAllTxOutputExceptOne(inputIndex: Int): RegularTx = {
      val updatedTxOut = tx.tx_out.take(inputIndex).zipWithIndex.map {
        case (txIn, index) =>
          (index == inputIndex)
            .option(txIn)
            .getOrElse(txIn.copy(value = -1, pk_script = ByteVector.empty))
      }

      tx.copy(tx_out = updatedTxOut)
    }
  }

  implicit class RichSegwitTx(tx: TxWitness) {
    def transactionId(): ByteVector = {
      // For witness stuff, whats the transaction id?
      TxWitness.codec(transactionVersion).encode(tx).toEither match {
        case Left(error) =>
          throw new RuntimeException(error.messageWithContext)
        case Right(v) =>
          v.toByteVector
      }
    }

    // https://github.com/bitcoin/bips/blob/master/bip-0143.mediawiki
    // https://github.com/bitcoin/bitcoin/blob/f8528134fc188abc5c7175a19680206964a8fade/src/script/interpreter.cpp#L1113
    def signingHash(pubKeyScript: Seq[ScriptElement], inputIndex: Int, amount: Long, sigHashType: SignatureHashType): Seq[Byte] = {
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
        val outputBytes: Array[Byte] = tx.tx_out.toArray.flatMap(TxOutWitness.codec.encode(_).toBytes)
        Hash.Hash256(outputBytes)
      } else if (sigHashType.SIGHASH_SINGLE() && inputIndex < tx.tx_out.length) {
        val txOut = tx.tx_out(inputIndex)
        val outputBytes = TxOutWitness.codec.encode(txOut).toBytes
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
  }

  implicit class RichTx(tx: Tx) {
    def serialized(): ByteVector = {
      tx match {
        case regularTx: RegularTx =>
          regularTx.transactionId()
        case txWitness: TxWitness =>
          txWitness.transactionId()
      }
    }
  }

}

