package it.softfork.bitcoin4s.external.blockcypher

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.Materializer
import it.softfork.bitcoin4s.crypto.Hash
import it.softfork.bitcoin4s.external.HttpSender
import it.softfork.bitcoin4s.external.blockcypher.Api._
import it.softfork.bitcoin4s.script.{Parser, ScriptElement}
import it.softfork.bitcoin4s.transaction.structure.{OutPoint, Hash => ScodecHash}
import it.softfork.bitcoin4s.transaction.{Tx, TxId, TxIn, TxOut}
import play.api.libs.json.{Format, JsError, JsSuccess, Json}
import scodec.bits.ByteVector
import it.softfork.bitcoin4s.ApiModels.scriptElementFormat

import scodec.bits._

import scala.collection.immutable.ArraySeq
import scala.concurrent.{ExecutionContext, Future}
import scodec.Attempt
import it.softfork.bitcoin4s.transaction.Script

// https://www.blockcypher.com/dev/bitcoin/#transaction-api
trait ApiInterface {
  def getTransaction(txId: TxId): Future[Option[Transaction]]
}

class Api(httpSender: HttpSender)(
  implicit
  ec: ExecutionContext,
  materializer: Materializer
) extends ApiInterface {
  override def getTransaction(txId: TxId): Future[Option[Transaction]] = {
    httpSender(HttpRequest(uri = rawTxUrl(txId))).flatMap { response =>
      if (response.status.isSuccess()) {
        transactionUnmarshaller(response.entity).map(Option.apply _)
      } else {
        Future.successful(None)
      }
    }
  }
}

class CachedApi(api: Api)(
  implicit
  system: ActorSystem,
  ec: ExecutionContext
) extends ApiInterface {
  implicit val transactionCacheActor = system.actorOf(TransactionCacheActor.props())

  override def getTransaction(txId: TxId): Future[Option[Transaction]] = {
    TransactionCacheActor.getTransaction(txId).flatMap { cachedTx =>
      cachedTx match {
        case None =>
          api.getTransaction(txId).map { maybeFreshTx =>
            maybeFreshTx match {
              case Some(freshTx) =>
                TransactionCacheActor.setTransaction(txId, freshTx)
                Some(freshTx)
              case None =>
                None
            }
          }
        case Some(tx) =>
          Future.successful(Some(tx))
      }
    }
  }
}

object Api {

  case class TransactionInput(
    prev_hash: String,
    output_index: Int,
    script: Option[String],
    parsed_script: Option[Seq[ScriptElement]],
    output_value: Long,
    sequence: Long,
    script_type: String,
    addresses: List[String],
    age: Long,
    witness: Option[List[String]] = None
  ) {
    val prevTxHash = ScodecHash(ByteVector(Hash.fromHex(prev_hash)))

    def toTxIn = TxIn(
      previous_output = OutPoint(prevTxHash, output_index),
      sig_script = script
        .map { s =>
          Script(ByteVector(Hash.fromHex(s)))
        }
        .getOrElse(Script.empty),
      sequence = sequence
    )

    def withParsedScript() = {
      val parsedScript = script.map { scriptStr =>
        Parser.parse("0x" + scriptStr)
      }

      copy(parsed_script = parsedScript)
    }
  }

  object TransactionInput {
    implicit val format: Format[TransactionInput] = Json.using[Json.WithDefaultValues].format[TransactionInput]
  }

  case class TransactionOutput(
    value: Long,
    script: String,
    parsed_script: Option[Seq[ScriptElement]],
    spent_by: Option[String],
    addresses: List[String],
    script_type: String
  ) {

    def toTxOut = TxOut(
      value = value,
      pk_script = Script(ByteVector(Parser.parse(ArraySeq.unsafeWrapArray(Hash.fromHex(script))).flatMap(_.bytes)))
    )

    def withParsedScript() = {
      val parsedScript = Parser.parse("0x" + script)
      copy(parsed_script = Some(parsedScript))
    }
  }

  object TransactionOutput {
    implicit val format: Format[TransactionOutput] = Json.using[Json.WithDefaultValues].format[TransactionOutput]
  }

  case class Transaction(
    block_hash: String,
    block_height: Long,
    block_index: Int,
    hash: String,
    hex: String,
    addresses: Seq[String],
    total: Long,
    fees: Long,
    size: Long,
    confirmed: ZonedDateTime,
    received: ZonedDateTime,
    ver: Int,
    lock_time: Long = 0,
    double_spend: Boolean,
    vin_sz: Int,
    vout_sz: Int,
    confirmations: Long,
    confidence: Int,
    inputs: Seq[TransactionInput],
    outputs: Seq[TransactionOutput]
  ) {

    println(hex)

//    val tx = Tx.codec(1).decodeValue(BitVector(Hash.fromHex(hex))) match {
//      case Attempt.Successful(tx) =>
//        println(tx.tx_witness)
//      case Attempt.Failure(e) =>
//        println(e)
//    }
    def toTx = {
//      Tx(
//        version = ver,
//        flag = true,
//        tx_in = inputs.map(_.toTxIn).toList,
//        tx_out = outputs.map(_.toTxOut).toList,
//        tx_witness = tx.tx_witness,
//        lock_time = lock_time
//      )
      val tx = Tx.codec(1).decodeValue(BitVector(Hash.fromHex(hex))) match {
        case Attempt.Successful(tx) =>
          tx
        case Attempt.Failure(e@_) =>
          println(s"niux: $e")
          throw new RuntimeException("niux")
      }

      tx
    }

    def withParsedScript() = {
      val inputsWithParsedScript = inputs.map(_.withParsedScript())
      val outputsWithParsedScript = outputs.map(_.withParsedScript())

      copy(inputs = inputsWithParsedScript, outputs = outputsWithParsedScript)
    }

    def withTransactionInputWitness() = {
      val tx = toTx
      println(s"input size not the same as witness size: ${inputs.size}")
      assert(inputs.size == tx.tx_witness.size, "input size not the same as witness size")

      val updatedInputs = inputs.zip(tx.tx_witness).map {
        case (input, witnesses) =>
          input.copy(witness = Some(witnesses.map(_.witness.value.toString)))
      }
      copy(inputs = updatedInputs)
    }
  }

  object Transaction {
    implicit val format: Format[Transaction] = Json.using[Json.WithDefaultValues].format[Transaction]
  }

  protected def rawTxUrl(txId: TxId) = Uri(s"https://api.blockcypher.com/v1/btc/main/txs/${txId.value}?limit=1000&includeHex=true")

  def parseTransaction(raw: String): Transaction = {
    Json.fromJson[Transaction](Json.parse(raw)) match {
      case JsSuccess(value, _) =>
        value
      case JsError(e) =>
        throw new RuntimeException(s"Failed to parse transaction $e")
    }
  }

  implicit protected val transactionUnmarshaller = Unmarshaller.stringUnmarshaller.map(parseTransaction)
}
