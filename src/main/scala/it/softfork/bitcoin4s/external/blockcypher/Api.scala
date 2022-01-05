package it.softfork.bitcoin4s.external.blockcypher

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.Materializer
import it.softfork.bitcoin4s.external.{ApiInterface, HttpSender, TransactionCacheActor}
import it.softfork.bitcoin4s.external.blockcypher.Api.{rawTxUrl, transactionUnmarshaller}
import it.softfork.bitcoin4s.script.{Parser, ScriptElement}
import it.softfork.bitcoin4s.transaction.structure.{Hash => ScodecHash}
import it.softfork.bitcoin4s.transaction.{Tx, TxId, TxRaw, TxWitness}
import play.api.libs.json.{Format, JsError, JsSuccess, Json}
import scodec.bits.ByteVector
import it.softfork.bitcoin4s.ApiModels.{
  scriptElementFormat,
  Transaction => ApiTransaction,
  TransactionInput => ApiTransactionInput,
  TransactionOutput => ApiTransactionOutput
}

import scala.concurrent.{ExecutionContext, Future}
import it.softfork.bitcoin4s.Utils.hexToBytes

class Api(httpSender: HttpSender)(implicit
  ec: ExecutionContext,
  materializer: Materializer
) extends ApiInterface {

  override def getTransaction(txId: TxId): Future[Option[ApiTransaction]] = {
    httpSender(HttpRequest(uri = rawTxUrl(txId))).flatMap { response =>
      if (response.status.isSuccess()) {
        transactionUnmarshaller(response.entity).map(_.toApiModel()).map(Option.apply _)
      } else {
        Future.successful(None)
      }
    }
  }
}

class CachedApi(api: Api)(implicit
  system: ActorSystem,
  ec: ExecutionContext
) extends ApiInterface {
  implicit val transactionCacheActor = system.actorOf(TransactionCacheActor.props())

  override def getTransaction(txId: TxId): Future[Option[ApiTransaction]] = {
    TransactionCacheActor.getTransaction(txId).flatMap {
      case None =>
        api.getTransaction(txId).map {
          case Some(freshTx) =>
            TransactionCacheActor.setTransaction(txId, freshTx)
            Some(freshTx)
          case None =>
            None
        }
      case Some(tx) =>
        Future.successful(Some(tx))
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
    val prevTxHash = ScodecHash(ByteVector(hexToBytes(prev_hash)))

    def withParsedScript() = {
      val parsedScript = script.map { scriptStr =>
        Parser.parse("0x" + scriptStr)
      }

      copy(parsed_script = parsedScript)
    }

    def toApiModel(): ApiTransactionInput = {
      ApiTransactionInput(
        prevHash = prev_hash,
        outputIndex = output_index,
        script = script,
        parsedScript = parsed_script,
        outputValue = output_value,
        sequence = sequence,
        scriptType = script_type,
        addresses = addresses,
        witness = witness
      )
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
    addresses: Option[List[String]],
    script_type: String
  ) {

    def withParsedScript() = {
      val parsedScript = Parser.parse("0x" + script)
      copy(parsed_script = Some(parsedScript))
    }

    def toApiModel(): ApiTransactionOutput = {
      ApiTransactionOutput(
        value = value,
        script = script,
        parsedScript = parsed_script,
        spentBy = spent_by,
        addresses = addresses.getOrElse(List.empty[String]),
        scriptType = script_type
      )
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
    val tx = Tx.fromHex(hex)

    def withParsedScript() = {
      val inputsWithParsedScript = inputs.map(_.withParsedScript())
      val outputsWithParsedScript = outputs.map(_.withParsedScript())

      copy(inputs = inputsWithParsedScript, outputs = outputsWithParsedScript)
    }

    def withWitness() = {
      val paddedWitness = tx.tx_witness
        .map(Option.apply)
        .padTo(inputs.length, Option.empty[List[TxWitness]])

      val updatedInputs = inputs.zip(paddedWitness).map { case (input, witnessMaybe) =>
        witnessMaybe
          .map { witness =>
            input.copy(witness = Some(witness.map(_.witness.hex)))
          }
          .getOrElse(input)
      }

      copy(inputs = updatedInputs)
    }

    def toApiModel(): ApiTransaction = {
      val updatedTransaction = this.withParsedScript().withWitness()

      ApiTransaction(
        hash = hash,
        hex = hex,
        txRaw = TxRaw(tx).toOption,
        total = total,
        size = size,
        confirmed = confirmed,
        version = ver,
        lockTime = lock_time,
        inputs = updatedTransaction.inputs.map(_.toApiModel()),
        outputs = updatedTransaction.outputs.map(_.toApiModel())
      )
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
