package it.softfork.bitcoin4s.external.blockcypher

import scala.collection.immutable.ArraySeq
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.control.Exception.allCatch

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import org.spongycastle.util.encoders.Hex

import it.softfork.bitcoin4s.ApiModels._
import it.softfork.bitcoin4s.ApiModels.InterpreterResultOut.NoResult
import it.softfork.bitcoin4s.external.ApiInterface
import it.softfork.bitcoin4s.script._
import it.softfork.bitcoin4s.script.SigVersion.{SIGVERSION_BASE, SIGVERSION_WITNESS_V0}
import it.softfork.bitcoin4s.transaction.TxId
import it.softfork.bitcoin4s.utils.hexToBytes

class Service(api: ApiInterface)(implicit
  ec: ExecutionContext
) extends StrictLogging {

  def getTransaction(txId: TxId): Future[Option[Transaction]] = {
    logger.info(s"Fetching transaction $txId")
    api.getTransaction(txId)
  }

  def getTransactionInput(txId: TxId, inputIndex: Int): Future[Option[TransactionInput]] = {
    getTransaction(txId).map { maybeTransaction =>
      maybeTransaction.flatMap { transaction =>
        allCatch.opt(transaction.inputs(inputIndex))
      }
    }
  }

  def getTransactionOutput(txId: TxId, outputIndex: Int): Future[Option[TransactionOutput]] = {
    getTransaction(txId).map { maybeTransaction =>
      maybeTransaction.flatMap { transaction =>
        allCatch.opt(transaction.outputs(outputIndex))
      }
    }
  }

  // scalastyle:off method.length
  def interpret(
    txId: TxId,
    inputIndex: Int,
    maybeStep: Option[Int] = None,
    flags: Seq[ScriptFlag] = Seq(ScriptFlag.SCRIPT_VERIFY_P2SH, ScriptFlag.SCRIPT_VERIFY_WITNESS)
  ): Future[Option[InterpreterOutcome]] = {
    getTransaction(txId).flatMap { maybeSpendingTx =>
      val maybeTxInput = maybeSpendingTx.flatMap { spendingTx =>
        allCatch.opt(spendingTx.inputs(inputIndex))
      }

      maybeTxInput match {
        case Some(txInput) =>
          val prevId = TxId(txInput.prevHash.toString)
          val outputIndex = txInput.outputIndex

          getTransactionOutput(prevId, outputIndex).map { maybeTxOutput =>
            maybeTxOutput.map { txOutout =>
              val scriptSig = txInput.script
                .map { s =>
                  Parser.parse(ArraySeq.unsafeWrapArray(hexToBytes(s)))
                }
                .getOrElse(Seq.empty)

              val scriptPubKey = Parser.parse(ArraySeq.unsafeWrapArray(hexToBytes(txOutout.script)))

              val witnessesStack = txInput.witness.map { rawWitnesses =>
                rawWitnesses.reverse.flatMap { rawWitness =>
                  allCatch.opt(Hex.decode(rawWitness)).map(ScriptConstant.apply)
                }
              }

              val amount = txOutout.value
              val sigVersion =
                if (witnessesStack.isEmpty) SIGVERSION_BASE else SIGVERSION_WITNESS_V0

              val initialState = InterpreterState(
                scriptPubKey = scriptPubKey,
                scriptSig = scriptSig,
                scriptWitnessStack = witnessesStack,
                flags = flags,
                transaction = maybeSpendingTx.get.tx, // Has to exist
                inputIndex = inputIndex,
                amount = amount,
                sigVersion = sigVersion
              )

              val outcome = Interpreter.create(verbose = false, maybeStep).run(initialState)

              logger.info(s"Interpreter finished with $outcome")

              outcome.map { case (finalState @ _, interpretedResult) =>
                InterpreterOutcome(
                  result = InterpreterResultOut.fromInterpreterResult(interpretedResult),
                  state = InterpreterStateOut.fromInterpreterState(finalState),
                  step = maybeStep
                )
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
  // scalastyle:on method.length

  def interpretStream(txId: TxId, inputIndex: Int): Source[InterpreterOutcome, Cancellable] = {
    case object Tick

    Source
      .tick(1.second, 2.seconds, Tick)
      .zipWithIndex
      .mapAsync(1) { case (_, step) =>
        interpret(txId, inputIndex, Some(step.toInt))
      }
      .map { maybeInterpreterOutcome =>
        maybeInterpreterOutcome.getOrElse {
          throw TransactionInputNotFound
        }
      }
      .takeWhile(_.result == NoResult, inclusive = true)
  }
}
