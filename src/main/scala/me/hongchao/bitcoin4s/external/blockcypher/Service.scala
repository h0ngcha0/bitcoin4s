package me.hongchao.bitcoin4s.external.blockcypher

import akka.stream.Materializer
import com.typesafe.scalalogging.StrictLogging
import me.hongchao.bitcoin4s.crypto.Hash
import me.hongchao.bitcoin4s.external.blockcypher.Api.{Transaction, TransactionInput, TransactionOutput}
import me.hongchao.bitcoin4s.script.SigVersion.{SIGVERSION_BASE, SIGVERSION_WITNESS_V0}
import me.hongchao.bitcoin4s.script._
import me.hongchao.bitcoin4s.transaction.TxId
import org.spongycastle.util.encoders.Hex

import scala.util.control.Exception.allCatch
import scala.concurrent.{ExecutionContext, Future}
import cats.implicits._


class Service(api: Api)(
  implicit
  ec: ExecutionContext,
  materializer: Materializer
) extends StrictLogging {

  def getTransaction(txId: TxId): Future[Transaction] = {
    logger.info(s"Fetching transaction $txId")
    api.getTransaction(txId)
  }

  def getTransactionInput(txId: TxId, inputIndex: Int): Future[Option[TransactionInput]] = {
    getTransaction(txId).map { transaction =>
      allCatch.opt(transaction.inputs(inputIndex))
    }
  }

  def getTransactionOutput(txId: TxId, outputIndex: Int): Future[Option[TransactionOutput]] = {
    getTransaction(txId).map { transaction =>
      allCatch.opt(transaction.outputs(outputIndex))
    }
  }

  def interpret(txId: TxId, inputIndex: Int): Future[Option[Option[Boolean]]] = {
    getTransaction(txId).flatMap { spendingTx =>

      val maybeTxInput = allCatch.opt(spendingTx.inputs(inputIndex))

      maybeTxInput match {
        case Some(txInput) =>
          val prevId = TxId(txInput.prevTxHash.toString)
          val outputIndex = txInput.output_index

          getTransactionOutput(prevId, outputIndex).map { maybeTxOutput =>
            maybeTxOutput.map { txOutout =>
              val scriptSig = txInput.script.map { s =>
                Parser.parse(Hash.fromHex(s))
              }.getOrElse(Seq.empty)

              val scriptPubKey = Parser.parse(Hash.fromHex(txOutout.script))

              val witnessesStack = txInput.witness.map { rawWitnesses =>
                rawWitnesses.reverse.flatMap { rawWitness =>
                  allCatch.opt(Hex.decode(rawWitness).toSeq).map(ScriptConstant.apply)
                }
              }

              val amount = txOutout.value

              // FIXME: make flags configurable
              val flags = Seq(ScriptFlag.SCRIPT_VERIFY_P2SH, ScriptFlag.SCRIPT_VERIFY_WITNESS)
              val sigVersion = if (witnessesStack.isEmpty) SIGVERSION_BASE else SIGVERSION_WITNESS_V0

              val initialState = InterpreterState(
                scriptPubKey = scriptPubKey,
                scriptSig = scriptSig,
                scriptWitnessStack = witnessesStack,
                flags = flags,
                transaction = spendingTx.toTx,
                inputIndex = inputIndex,
                amount = amount,
                sigVersion = sigVersion
              )

              val outcome = Interpreter.create(verbose = true).run(initialState)

              logger.info(s"Interpreter finished with $outcome")

              outcome.map {
                case (finalState@_, interpretedResult) =>
                  interpretedResult
              } match {
                case Left(e) =>
                  throw e
                case Right(result) =>
                  result
              }
            }
          }

        case None =>
          Future.successful(None)
      }
    }
  }

}
