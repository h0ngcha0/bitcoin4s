package me.hongchao.bitcoin4s.script

import io.github.yzernik.bitcoinscodec.messages.Tx
import me.hongchao.bitcoin4s.script.CryptoOp.OP_CODESEPARATOR
import scodec.bits.ByteVector
import me.hongchao.bitcoin4s.Utils._
import me.hongchao.bitcoin4s.crypto.Hash
import me.hongchao.bitcoin4s.script.SigVersion.{SIGVERSION_BASE, SIGVERSION_WITNESS_V0}

object TransactionOps {
  val transactionVersion = 1

  implicit class RichTx(tx: Tx) {
    def transactionId(): ByteVector = {
      Tx.codec(transactionVersion).encode(tx).toEither match {
        case Left(error) =>
          throw new RuntimeException(error.messageWithContext)
        case Right(v) =>
          v.toByteVector
      }
    }

    def signingHash(
      pubKeyScript: Seq[ScriptElement],
      inputIndex: Int,
      sigHashType: SignatureHashType,
      sigVersion: SigVersion
    ): Seq[Byte] = sigVersion match {
      case SIGVERSION_BASE =>
        normalSigningHash(pubKeyScript, inputIndex, sigHashType)
      case SIGVERSION_WITNESS_V0 =>
        segwitSigningHash()
    }

    def normalSigningHash(pubKeyScript: Seq[ScriptElement], inputIndex: Int, sigHashType: SignatureHashType): Seq[Byte] = {
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

      Hash.Hash256(serialisedTx ++ uint32ToBytes(sigHashType.value))
    }

    def segwitSigningHash(): Seq[Byte] = {
      throw new NotImplementedError("Segwit hashing for transaction is not implemented")
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
}

